# 5-Minute Quick Start

[中文](zh-CN/quickstart.md) | English

This guide helps you run a Java MCP Server locally and connect it to an Agent in about 5 minutes. Start with the SDK demo if you do not have Redis or MySQL ready.

## Prerequisites

- Java 17+
- Maven 3.9+
- An MCP-compatible Agent, such as Claude Desktop or Claude Code
- Optional: local Redis or MySQL for ready-to-use data source servers

## Option A: SDK quick-start server, fastest path

This path has no database dependency.

Build the quick-start server:

```bash
mvn package -pl mcp-examples/quick-start -am -DskipTests
```

Add this to your Agent MCP configuration:

```json
{
  "mcpServers": {
    "quick-start": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-java/mcp-examples/quick-start/target/quick-start-0.1.1.jar"
      ]
    }
  }
}
```

Restart the Agent and try:

```text
Use the quick-start MCP server to greet Alice and get the current time.
```

```text
Use the quick-start MCP server to calculate 12 multiplied by 8.
```

Expected result: the Agent calls tools exposed by the quick-start server.

Available capabilities:

- Tools: `greet`, `current_time`, `calculate`, `text_transform`
- Resource: `server://info`

## Option B: Custom server template

Use this path when you want a copyable starting point for your own MCP Server.

Build the template:

```bash
mvn package -pl mcp-examples/custom-server-template -am -DskipTests
```

Add this to your Agent MCP configuration:

```json
{
  "mcpServers": {
    "custom-template": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-java/mcp-examples/custom-server-template/target/custom-server-template-0.1.1.jar"
      ]
    }
  }
}
```

Restart the Agent and try:

```text
Use the custom-template MCP server to say hello to Alice.
```

Then edit [`CustomMcpServer.java`](../mcp-examples/custom-server-template/src/main/java/com/mcp/example/template/CustomMcpServer.java) to add your own tools, resources, and prompts.

## Option C: Redis server

Requires a local Redis instance.

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
        "/absolute/path/to/mcp-java/mcp-server-collection/mcp-server-redis/target/mcp-server-redis-0.1.1.jar"
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

## Option D: MySQL server

Requires a local MySQL instance.

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
        "/absolute/path/to/mcp-java/mcp-server-collection/mcp-server-mysql/target/mcp-server-mysql-0.1.1.jar"
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
- Restart the Agent after editing MCP configuration.
- Keep passwords in local Agent configuration or environment variables only.
- Test Redis/MySQL with local or non-production data first.
- In stdio mode, stdout is reserved for MCP JSON-RPC messages; server logs are written to stderr.
- Redis `keys` uses narrow namespaced patterns such as `demo:*`; broad patterns like `*` are rejected.
- MySQL `execute` only accepts `INSERT` and `UPDATE`; destructive SQL is rejected.

## If it does not work

Start with [Troubleshooting](troubleshooting.md). The most common issues are a non-absolute jar path, forgetting to rebuild the jar, forgetting to restart the Agent, or writing logs to stdout.

## Next steps

- [Custom Server Template](../mcp-examples/custom-server-template)
- [Redis Server Guide](redis-server.md)
- [MySQL Server Guide](mysql-server.md)
- [Security Guide](security.md)
- [Troubleshooting](troubleshooting.md)
