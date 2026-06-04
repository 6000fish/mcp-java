# 5 分钟快速开始

中文 | [English](../quickstart.md)

本指南帮助你在约 5 分钟内本地启动一个可直接使用的 MCP Server，并连接到 Agent。

## 前置条件

- Java 17+
- Maven 3.9+
- 本地 Redis 或 MySQL 实例
- 支持 MCP 的 Agent，例如 Claude Desktop 或 Claude Code

## 方案 A：Redis Server，最快路径

构建 Redis MCP Server：

```bash
mvn package -pl mcp-server-collection/mcp-server-redis -am -DskipTests
```

在 Agent MCP 配置中添加：

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

重启 Agent 后尝试：

```text
Set Redis key demo:greeting to hello-mcp, then read it back.
```

预期结果：Agent 调用 Redis 工具，并返回已写入的值。

## 方案 B：MySQL Server

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

重启 Agent 后尝试：

```text
Insert 3 test users into the users table, then query them back.
```

预期结果：Agent 调用 MySQL 工具，并返回插入后的数据行。

## 注意事项

- Agent 配置中请使用 jar 的绝对路径。
- 密码只放在本地 Agent 配置或环境变量中。
- stdio 模式下 stdout 保留给 MCP JSON-RPC 协议消息，服务日志会写入 stderr。

## 下一步

- [Redis Server 使用手册](redis-server.md)
- [MySQL Server 使用手册](mysql-server.md)
- [安全说明](security.md)
- [故障排查](troubleshooting.md)
