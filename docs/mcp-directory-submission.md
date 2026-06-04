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

Java SDK and ready-to-use MCP Servers for connecting AI Agents to MySQL and Redis over the Model Context Protocol.

## Long description

MCP Java SDK provides a Java implementation of the Model Context Protocol with stdio transport, annotation-driven tool registration, Spring Boot integration, and production-oriented ready-to-use MCP Servers. The first server release focuses on MySQL and Redis so developers can connect Agents to local or self-hosted data services in minutes.

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

### Tools

- `query`
- `execute`
- `list_databases`
- `list_tables`
- `describe_table`
- `explain_query`
- `get_table_status`

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

### Tools

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

### Safety notes

- `del` is disabled by default.
- Broad key patterns such as `*` are rejected.
- Collection reads and returned key counts are bounded.

## Submission checklist

- [ ] Confirm repository README links to Quick Start and server docs.
- [ ] Confirm release artifacts are available from GitHub Releases.
- [ ] Confirm example Agent configs use absolute jar paths.
- [ ] Confirm secrets are documented as local-only config.
- [ ] Submit MySQL server listing.
- [ ] Submit Redis server listing.
- [ ] Add badges or directory links back to README after approval.
