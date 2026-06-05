# 5 分钟快速开始

中文 | [English](../quickstart.md)

本指南帮助你在约 5 分钟内本地启动一个 Java MCP Server，并连接到 Agent。如果你还没有 Redis 或 MySQL，建议先从 SDK demo 开始。

## 前置条件

- Java 17+
- Maven 3.9+
- 支持 MCP 的 Agent，例如 Claude Desktop 或 Claude Code
- 可选：本地 Redis 或 MySQL，用于 ready-to-use 数据源 Server

## 方案 A：SDK quick-start Server，最快路径

这个方案不依赖数据库。

构建 quick-start Server：

```bash
mvn package -pl mcp-examples/quick-start -am -DskipTests
```

在 Agent MCP 配置中添加：

```json
{
  "mcpServers": {
    "quick-start": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-java/mcp-examples/quick-start/target/quick-start-0.1.0.jar"
      ]
    }
  }
}
```

重启 Agent 后尝试：

```text
Use the quick-start MCP server to greet Alice and get the current time.
```

```text
Use the quick-start MCP server to calculate 12 multiplied by 8.
```

预期结果：Agent 调用 quick-start Server 暴露的工具。

可用能力：

- 工具：`greet`、`current_time`、`calculate`、`text_transform`
- 资源：`server://info`

## 方案 B：自定义 Server 模板

如果你想复制一个起点来开发自己的 MCP Server，使用这个方案。

构建模板：

```bash
mvn package -pl mcp-examples/custom-server-template -am -DskipTests
```

在 Agent MCP 配置中添加：

```json
{
  "mcpServers": {
    "custom-template": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-java/mcp-examples/custom-server-template/target/custom-server-template-0.1.0.jar"
      ]
    }
  }
}
```

重启 Agent 后尝试：

```text
Use the custom-template MCP server to say hello to Alice.
```

然后修改 [`CustomMcpServer.java`](../../mcp-examples/custom-server-template/src/main/java/com/mcp/example/template/CustomMcpServer.java)，添加你自己的工具、资源和 Prompt。

## 方案 C：Redis Server

需要本地 Redis 实例。

构建 Redis MCP Server：

```bash
mvn package -pl mcp-server-collection/mcp-server-redis -am -DskipTests
```

在 Agent MCP 配置中添加：

```json
{
  "mcpServers": {
    "redis": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-java/mcp-server-collection/mcp-server-redis/target/mcp-server-redis-0.1.0.jar"
      ],
      "env": {
        "REDIS_HOST": "localhost",
        "REDIS_PORT": "6379",
        "REDIS_PASSWORD": ""
      }
    }
  }
}
```

重启 Agent 后尝试：

```text
Set Redis key demo:greeting to hello-mcp, then read it back.
```

预期结果：Agent 调用 Redis 工具，并返回已写入的值。

## 方案 D：MySQL Server

需要本地 MySQL 实例。

构建 MySQL MCP Server：

```bash
mvn package -pl mcp-server-collection/mcp-server-mysql -am -DskipTests
```

创建测试表：

```sql
CREATE DATABASE IF NOT EXISTS mcp_demo;
USE mcp_demo;
CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  age INT NOT NULL
);
```

在 Agent MCP 配置中添加：

```json
{
  "mcpServers": {
    "mysql": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-java/mcp-server-collection/mcp-server-mysql/target/mcp-server-mysql-0.1.0.jar"
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

重启 Agent 后尝试：

```text
Insert 3 test users into the users table, then query them back.
```

预期结果：Agent 调用 MySQL 工具，并返回插入后的数据行。

## 注意事项

- Agent 配置中请使用 jar 的绝对路径。
- 修改 MCP 配置后需要重启 Agent。
- 密码只放在本地 Agent 配置或环境变量中。
- Redis/MySQL 请先用本地或非生产数据测试。
- stdio 模式下 stdout 保留给 MCP JSON-RPC 协议消息，服务日志会写入 stderr。
- Redis `keys` 使用 `demo:*` 这类窄命名空间模式，拒绝 `*` 等宽泛 pattern。
- MySQL `execute` 只接受 `INSERT` 和 `UPDATE`，拒绝破坏性 SQL。

## 如果跑不起来

先看[故障排查](troubleshooting.md)。最常见的问题是 jar 路径不是绝对路径、忘记重新构建 jar、忘记重启 Agent，或者服务把日志写到了 stdout。

## 下一步

- [自定义 Server 模板](../../mcp-examples/custom-server-template)
- [Redis Server 使用手册](redis-server.md)
- [MySQL Server 使用手册](mysql-server.md)
- [安全说明](security.md)
- [故障排查](troubleshooting.md)
