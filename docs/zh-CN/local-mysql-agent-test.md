# 本地 MySQL MCP Agent 实操验证手册

这是一份临时验证文档，用于在发布 MCP 市场前，确认 `mcp-server-mysql-0.1.0.jar` 可以被 Agent 正常启动，并通过大模型自然语言访问本地 MySQL。

验证完成后可以删除本文件。

## 1. 验证目标

确认完整链路可用：

```text
用户自然语言
→ 大模型理解意图
→ Agent 选择 mysql MCP tool
→ mcp-server-mysql-0.1.0.jar 访问本地 MySQL
→ MCP Server 返回结果
→ 大模型总结回复用户
```

本次重点验证：

- Agent 能识别 MySQL MCP Server
- Agent 能看到 MySQL MCP tools
- Agent 能通过自然语言触发工具调用
- MySQL Server 能正常连接本地数据库
- 能完成表发现、表结构查看、插入数据、查询数据

## 2. 前置条件

确认本机已具备：

- Java 17+
- Maven 3.9+
- 本地 MySQL 正在运行
- 一个支持 MCP 的 Agent，例如 Claude Desktop、Claude Code 或其他 MCP Client
- 当前项目已能正常构建

检查 Java：

```bash
java -version
```

检查 Maven：

```bash
mvn -version
```

检查 MySQL：

```bash
mysql -h localhost -P 3306 -u root -p
```

## 3. 构建 MySQL MCP Server jar

在 `mcp-java` 项目根目录执行：

```bash
mvn package -pl mcp-server-collection/mcp-server-mysql -am -DskipTests
```

构建成功后，确认 jar 存在：

```text
mcp-server-collection/mcp-server-mysql/target/mcp-server-mysql-0.1.0.jar
```

建议复制这个 jar 的绝对路径，后面配置 Agent 时使用。

示例：

```text
C:/Users/x/mcp-java/mcp-server-collection/mcp-server-mysql/target/mcp-server-mysql-0.1.0.jar
```

## 4. 准备测试数据库

登录 MySQL 后执行：

```sql
CREATE DATABASE IF NOT EXISTS mcp_demo;
USE mcp_demo;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  age INT NOT NULL,
  city VARCHAR(64) DEFAULT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

可选：清空测试数据，方便重复验证：

```sql
TRUNCATE TABLE users;
```

注意：`TRUNCATE` 是你手动在 MySQL 客户端里执行的初始化动作，MySQL MCP Server 的工具调用会拒绝 `TRUNCATE` 等危险 SQL。

## 5. 配置 Agent MCP Server

把下面配置加入你的 Agent MCP 配置文件。

请把 jar 路径和 MySQL 密码替换成你本机真实值。

```json
{
  "mcpServers": {
    "mysql": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "C:/Users/x/mcp-java/mcp-server-collection/mcp-server-mysql/target/mcp-server-mysql-0.1.0.jar"
      ],
      "env": {
        "MYSQL_HOST": "localhost",
        "MYSQL_PORT": "3306",
        "MYSQL_DATABASE": "mcp_demo",
        "MYSQL_USERNAME": "root",
        "MYSQL_PASSWORD": "your_password"
      }
    }
  }
}
```

配置说明：

| 配置 | 说明 |
|---|---|
| `command` | 固定为 `java` |
| `args` | 使用 `-jar` 启动 MySQL MCP Server jar |
| `MYSQL_HOST` | MySQL 地址，本地通常是 `localhost` |
| `MYSQL_PORT` | MySQL 端口，默认 `3306` |
| `MYSQL_DATABASE` | 默认连接数据库，本例为 `mcp_demo` |
| `MYSQL_USERNAME` | MySQL 用户名 |
| `MYSQL_PASSWORD` | MySQL 密码 |

配置完成后，重启 Agent。

## 6. 验证 Agent 能看到 MCP 工具

重启 Agent 后，先问一个轻量问题：

```text
请查看 mysql MCP server 当前数据库有哪些表。
```

预期结果：

- Agent 会调用 `list_tables`
- 返回结果里能看到 `users`
- Agent 用自然语言告诉你当前数据库有哪些表

如果 Agent 提示找不到工具或找不到 MCP Server，先看本文末尾故障排查。

## 7. 验证表结构查看

继续问：

```text
请使用 mysql MCP server 查看 users 表结构，并告诉我每个字段的含义。
```

预期结果：

- Agent 会调用 `describe_table`
- 返回字段包括：`id`、`name`、`age`、`city`、`created_at`
- Agent 能总结表结构

## 8. 验证插入数据

继续问：

```text
请使用 mysql MCP server 往 users 表插入 3 条测试用户数据，要求名字、年龄、城市都不一样。插入完成后告诉我影响了几行。
```

预期结果：

- Agent 会生成 `INSERT INTO users ...` SQL
- Agent 会调用 `execute`
- 返回类似：`Rows affected: 3`

注意：当前 MySQL MCP Server 允许安全的 `INSERT` 和 `UPDATE`，但会拒绝 `DELETE`、`DROP`、`ALTER`、`TRUNCATE` 等危险操作。

## 9. 验证查询数据

继续问：

```text
请使用 mysql MCP server 查询 users 表中最新的 10 条数据，并用中文总结这些用户信息。
```

预期结果：

- Agent 会调用 `query`
- SQL 应该是单条 `SELECT`
- Agent 会把查询结果总结成自然语言

## 10. 验证安全限制

可以尝试危险操作，确认会被拒绝：

```text
请使用 mysql MCP server 删除 users 表里的所有数据。
```

预期结果：

- Agent 即使尝试调用工具，Server 也会拒绝 `DELETE`
- 返回错误中应包含不允许执行的原因

这一步能证明 Server 有基本安全护栏。

## 11. 推荐完整验收 Prompt

如果你想一次性验证完整流程，可以直接问：

```text
请使用 mysql MCP server 完成以下操作：
1. 查看当前数据库有哪些表；
2. 查看 users 表结构；
3. 往 users 表插入 3 条不同的测试用户数据；
4. 查询 users 表最新 10 条数据；
5. 用中文总结执行结果。
```

预期结果：

- Agent 至少调用 `list_tables`、`describe_table`、`execute`、`query`
- 最终回复中包含插入结果和查询结果总结

## 12. 常见问题

### 12.1 Agent 找不到 mysql MCP server

检查：

- MCP 配置文件是否保存成功
- Agent 是否已经重启
- jar 路径是否为绝对路径
- jar 文件是否真实存在
- JSON 是否格式正确

### 12.2 Server 启动失败

手动运行 jar 看是否能启动：

```bash
java -jar C:/Users/x/mcp-java/mcp-server-collection/mcp-server-mysql/target/mcp-server-mysql-0.1.0.jar
```

如果 MySQL 配置缺失，可以临时通过环境变量运行，或优先通过 Agent 配置里的 `env` 设置。

### 12.3 MySQL 连接失败

检查：

- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- MySQL 用户是否有访问 `mcp_demo` 的权限

可以用命令验证：

```bash
mysql -h localhost -P 3306 -u root -p mcp_demo
```

如果手动 PowerShell 设置 `$env:MYSQL_PASSWORD` 后能启动，但 Agent 仍然握手失败，说明 Agent 进程没有继承你当前 PowerShell 会话里的临时环境变量。可以检查用户级环境变量是否存在：

```powershell
[bool][Environment]::GetEnvironmentVariable("MYSQL_PASSWORD", "User")
```

如果返回 `False`，需要重新设置用户级环境变量，并完全退出后重启 Agent：

```powershell
[Environment]::SetEnvironmentVariable("MYSQL_HOST", "localhost", "User")
[Environment]::SetEnvironmentVariable("MYSQL_PORT", "3306", "User")
[Environment]::SetEnvironmentVariable("MYSQL_DATABASE", "mcp_demo", "User")
[Environment]::SetEnvironmentVariable("MYSQL_USERNAME", "root", "User")
[Environment]::SetEnvironmentVariable("MYSQL_PASSWORD", "你的真实密码", "User")
```

验证完成后如需清理密码：

```powershell
[Environment]::SetEnvironmentVariable("MYSQL_PASSWORD", $null, "User")
```

### 12.4 Agent 没有自动调用工具

尝试在 Prompt 中明确指定：

```text
请使用 mysql MCP server，不要只给 SQL，请实际调用工具查询 users 表。
```

### 12.5 插入失败

检查：

- 表是否存在
- 字段是否匹配
- SQL 是否包含被拒绝的危险关键字
- 是否生成了多语句 SQL

MySQL MCP Server 只允许单条安全 `INSERT` 或 `UPDATE`。

### 12.6 日志污染 Agent 输出

stdio 模式下 stdout 必须只输出 MCP JSON-RPC 协议消息，日志应写 stderr。

当前 MySQL Server jar 已包含 `logback.xml`，会把日志写到 stderr。如果后续改代码，不要在 stdio server 中添加 `System.out.println`。

## 13. 验证通过标准

满足以下条件即可认为本地验证通过：

- Agent 能识别 mysql MCP server
- Agent 能调用 `list_tables`
- Agent 能调用 `describe_table`
- Agent 能调用 `execute` 插入数据
- Agent 能调用 `query` 查询数据
- Agent 能把结果用自然语言总结给用户
- 危险 SQL 会被拒绝

通过后，可以继续准备：

- GitHub Release 上传 `mcp-server-mysql-0.1.0.jar`
- MCP 市场/目录提交材料
- 发布前 README 和文档最终检查
