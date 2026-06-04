# 5-Minute Quick Start

[中文](zh-CN/quickstart.md) | English

This guide helps you run a ready-to-use MCP Server locally and connect it to an Agent in about 5 minutes.

## Prerequisites

- Java 17+
- Maven 3.9+
- A local Redis or MySQL instance
- An MCP-compatible Agent, such as Claude Desktop or Claude Code

## Option A: Redis server, fastest path

Build the Redis MCP Server:

```bash
mvn package -pl mcp-server-collection/mcp-server-redis -am -DskipTests
```

Add this to your Agent MCP configuration:

```json
{
  "mcpServers": {
    "redis": {
      "type": "stdio",
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

Restart the Agent and try:

```text
Set Redis key demo:greeting to hello-mcp, then read it back.
```

Expected result: the Agent calls Redis tools and replies with the stored value.

## Option B: MySQL server

Build the MySQL MCP Server:

```bash
mvn package -pl mcp-server-collection/mcp-server-mysql -am -DskipTests
```

Create a test table:

```sql
CREATE DATABASE IF NOT EXISTS mcp_demo;
USE mcp_demo;
CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  age INT NOT NULL
);
```

Add this to your Agent MCP configuration:

```json
{
  "mcpServers": {
    "mysql": {
      "type": "stdio",
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

Restart the Agent and try:

```text
Insert 3 test users into the users table, then query them back.
```

Expected result: the Agent calls MySQL tools and returns the inserted rows.

## Notes

- Use absolute jar paths in Agent configuration.
- Keep passwords in local Agent configuration or environment variables only.
- In stdio mode, stdout is reserved for MCP JSON-RPC messages; server logs are written to stderr.

## Next steps

- [Redis Server Guide](redis-server.md)
- [MySQL Server Guide](mysql-server.md)
- [Security Guide](security.md)
- [Troubleshooting](troubleshooting.md)
