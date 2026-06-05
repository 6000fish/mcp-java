# MCP Directory Submission Materials

[中文](zh-CN/mcp-directory-submission.md) | English

Use this material when submitting the MySQL and Redis MCP Servers to MCP directories or marketplaces such as Smithery, Glama, PulseMCP, MCP.so, and Awesome MCP Servers.

## Project

- Name: MCP Java SDK
- Repository: https://github.com/6000fish/mcp-java
- License: MIT
- Runtime: Java 17+
- Transport: stdio
- Primary artifacts:
  - `mcp-server-mysql-0.1.0.jar`
  - `mcp-server-redis-0.1.0.jar`

## Short description

Java toolkit for building custom MCP Servers, with ready-to-use MySQL and Redis servers for connecting AI Agents to databases and caches over the Model Context Protocol.

## Long description

MCP Java SDK provides a Java implementation of the Model Context Protocol with stdio transport, annotation-driven tool registration, Spring Boot integration, and a copyable custom server template for quickly building custom MCP Servers. The first ready-to-use server release focuses on MySQL and Redis so developers can connect Agents to local or self-hosted data services in minutes.

The MySQL server supports database discovery, table inspection, read-only SELECT queries, safe INSERT/UPDATE operations, EXPLAIN, and table status. The Redis server supports common key/value, hash, list, set, metadata, and diagnostic operations with guardrails for broad scans and destructive commands.

## Tags

```text
mcp, model-context-protocol, java, mysql, redis, database, cache, agent-tools, stdio
```

## MySQL listing

### Name

MySQL MCP Server for Java

### Description

Connect MCP-compatible Agents to MySQL for database discovery, table inspection, safe SELECT queries, controlled INSERT/UPDATE operations, EXPLAIN plans, and table status metadata.

### Install

```bash
mvn package -pl mcp-server-collection/mcp-server-mysql -am -DskipTests
```

### Configuration

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

### Tools

- `query(sql)`
- `execute(sql)`
- `list_databases()`
- `list_tables(database?)`
- `describe_table(table, database?)`
- `explain_query(sql)`
- `get_table_status(database?)`

### Safety notes

- `query` only accepts a single `SELECT` statement.
- `execute` only accepts `INSERT` and `UPDATE`.
- Dangerous SQL such as `DELETE`, `DROP`, `ALTER`, `TRUNCATE`, permission changes, and multi-statement SQL is rejected.

## Redis listing

### Name

Redis MCP Server for Java

### Description

Connect MCP-compatible Agents to Redis for safe key/value, hash, list, set, metadata, and diagnostic operations over stdio.

### Install

```bash
mvn package -pl mcp-server-collection/mcp-server-redis -am -DskipTests
```

### Configuration

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

### Tools

- `get(key)`
- `set(key, value, ttl?)`
- `del(keys)` disabled by default
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

### Safety notes

- `del` is disabled by default.
- Broad key patterns such as `*` are rejected.
- Collection reads and returned key counts are bounded.

## Compatibility notes

- Stdio configuration should include `"type": "stdio"` for clients that require an explicit transport type.
- Verified with Claude Code using MySQL and Redis stdio servers.
- Verified with Codex using the MySQL stdio server, including `tools/list`, `tools/call`, and client `_meta` fields.
- Tool definitions include `inputSchema`, stdio responses omit null fields, and server logs are written to stderr.

## Submission checklist

- [x] Confirm repository README links to Quick Start, custom server template, and server docs.
- [x] Confirm release artifacts are available from GitHub Releases.
- [x] Confirm example Agent configs use absolute jar paths and `"type": "stdio"`.
- [x] Confirm secrets are documented as local-only config.
- [x] Confirm Claude Code and Codex compatibility notes are included.
- [ ] Submit MySQL server listing.
- [ ] Submit Redis server listing.
- [ ] Add badges or directory links back to README after approval.
