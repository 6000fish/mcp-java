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

Java SDK 和开箱即用 MCP Server，用于通过 Model Context Protocol 将 AI Agent 连接到 MySQL 和 Redis。

## 详细描述

MCP Java SDK 提供 Model Context Protocol 的 Java 实现，支持 stdio 传输、注解驱动工具注册、Spring Boot 集成，以及面向生产使用的开箱即用 MCP Server。首个 Server 发布版本聚焦 MySQL 和 Redis，帮助开发者在几分钟内将 Agent 连接到本地或自托管的数据服务。

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
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-server-mysql-0.1.0.jar"
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

- `query`
- `execute`
- `list_databases`
- `list_tables`
- `describe_table`
- `explain_query`
- `get_table_status`

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
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-server-redis-0.1.0.jar"
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

- `get`
- `set`
- `hget`
- `hset`
- `hgetall`
- `keys`
- `type`
- `ttl`
- `lrange`
- `llen`
- `scard`
- `smembers`
- `info`
- `dbsize`

### 安全说明

- 默认禁用 `del`。
- 拒绝 `*` 等宽泛 key pattern。
- 限制集合读取和返回 key 数量。

## 提交检查清单

- [ ] 确认仓库 README 已链接 Quick Start 和 Server 文档。
- [ ] 确认 GitHub Releases 中已有发布产物。
- [ ] 确认示例 Agent 配置使用绝对 jar 路径。
- [ ] 确认敏感信息说明为仅本地配置。
- [ ] 提交 MySQL Server 条目。
- [ ] 提交 Redis Server 条目。
- [ ] 审核通过后，将目录徽章或链接补回 README。
