# MCP 市场提交材料

中文 | [English](../mcp-directory-submission.md)

提交 MySQL 和 Redis MCP Server 到 MCP 目录或市场时可使用本材料，例如 Smithery、Glama、PulseMCP、MCP.so 和 Awesome MCP Servers。

## 项目

- 名称：MCP Java SDK
- 仓库：https://github.com/6000fish/mcp-java
- 许可证：MIT
- 运行环境：Java 17+
- 传输方式：stdio
- 主要产物：
  - `mcp-server-mysql-0.1.0.jar`
  - `mcp-server-redis-0.1.0.jar`

## 简短描述

面向 Java 开发者的自定义 MCP Server 开发工具包，并提供开箱即用的 MySQL 和 Redis Server，用于通过 Model Context Protocol 将 AI Agent 连接到数据库和缓存。

## 详细描述

MCP Java SDK 提供 Model Context Protocol 的 Java 实现，支持 stdio 传输、注解驱动工具注册、Spring Boot 集成和可复制的自定义 Server 模板，用于快速构建自定义 MCP Server。首个开箱即用 Server 发布版本聚焦 MySQL 和 Redis，帮助开发者在几分钟内将 Agent 连接到本地或自托管的数据服务。

MySQL Server 支持数据库发现、表结构查看、只读 SELECT 查询、安全 INSERT/UPDATE 操作、EXPLAIN 和表状态信息。Redis Server 支持常见 key/value、hash、list、set、元数据和诊断操作，并对宽泛扫描和破坏性命令提供安全限制。

## 标签

```text
mcp, model-context-protocol, java, mysql, redis, database, cache, agent-tools, stdio
```

## MySQL 条目

### 名称

MySQL MCP Server for Java

### 描述

将 MCP 兼容 Agent 连接到 MySQL，支持数据库发现、表结构查看、安全 SELECT 查询、受控 INSERT/UPDATE 操作、EXPLAIN 执行计划和表状态元数据。

### 安装

```bash
mvn package -pl mcp-server-collection/mcp-server-mysql -am -DskipTests
```

### 配置

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

### 工具

- `query(sql)`
- `execute(sql)`
- `list_databases()`
- `list_tables(database?)`
- `describe_table(table, database?)`
- `explain_query(sql)`
- `get_table_status(database?)`

### 安全说明

- `query` 只接受单条 `SELECT` 语句。
- `execute` 只接受 `INSERT` 和 `UPDATE`。
- 拒绝 `DELETE`、`DROP`、`ALTER`、`TRUNCATE`、权限变更和多语句 SQL 等危险操作。

## Redis 条目

### 名称

Redis MCP Server for Java

### 描述

将 MCP 兼容 Agent 连接到 Redis，通过 stdio 提供安全的 key/value、hash、list、set、元数据和诊断操作。

### 安装

```bash
mvn package -pl mcp-server-collection/mcp-server-redis -am -DskipTests
```

### 配置

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

### 工具

- `get(key)`
- `set(key, value, ttl?)`
- `del(keys)` 默认禁用
- `keys(pattern)`
- `type(key)`
- `ttl(key)`
- `hget(key, field)`
- `hset(key, field, value)`
- `hgetall(key)`
- `lrange(key, start, stop)`
- `llen(key)`
- `scard(key)`
- `smembers(key)`
- `info(section)`
- `dbsize()`

### 安全说明

- 默认禁用 `del`。
- 拒绝 `*` 等宽泛 key pattern。
- 限制集合读取和返回 key 数量。

## 兼容性说明

- stdio 配置建议包含 `"type": "stdio"`，兼容需要显式传输类型的客户端。
- 已使用 Claude Code 验证 MySQL 和 Redis stdio server。
- 已使用 Codex 验证 MySQL stdio server，包括 `tools/list`、`tools/call` 和客户端 `_meta` 字段。
- 工具定义包含 `inputSchema`，stdio 响应省略 null 字段，服务日志写入 stderr。

## 提交检查清单

- [x] 确认仓库 README 已链接 Quick Start、自定义 Server 模板和 Server 文档。
- [x] 确认 GitHub Releases 中已有发布产物。
- [x] 确认示例 Agent 配置使用绝对 jar 路径和 `"type": "stdio"`。
- [x] 确认敏感信息说明为仅本地配置。
- [x] 确认已包含 Claude Code 和 Codex 兼容性说明。
- [ ] 提交 MySQL Server 条目。
- [ ] 提交 Redis Server 条目。
- [ ] 审核通过后，将目录徽章或链接补回 README。
