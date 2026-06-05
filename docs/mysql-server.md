# MySQL MCP Server Guide

[中文](zh-CN/mysql-server.md) | English

The MySQL MCP Server exposes database discovery, safe reads, and controlled data modifications as MCP tools over stdio.

## Build

```bash
mvn package -pl mcp-server-collection/mcp-server-mysql -am -DskipTests
```

The executable jar is generated at:

```text
mcp-server-collection/mcp-server-mysql/target/mcp-server-mysql-0.1.0.jar
```

## Configuration

Environment variables:

| Variable | Required | Default | Description |
|---|---:|---|---|
| `MYSQL_HOST` | No | `localhost` | MySQL host |
| `MYSQL_PORT` | No | `3306` | MySQL port |
| `MYSQL_DATABASE` | No | `test` | Default database |
| `MYSQL_USERNAME` | No | `root` | MySQL username |
| `MYSQL_PASSWORD` | No | empty | MySQL password |

## Agent configuration

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

## Demo database

```sql
CREATE DATABASE IF NOT EXISTS mcp_demo;
USE mcp_demo;
CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  age INT NOT NULL
);
```

## Tools

| Tool | Description |
|---|---|
| `query(sql)` | Execute a read-only `SELECT` query and return rows as JSON |
| `execute(sql)` | Execute a safe `INSERT` or `UPDATE` statement |
| `list_databases()` | List visible databases |
| `list_tables(database?)` | List tables in the current or specified database |
| `describe_table(table, database?)` | Describe table columns and types |
| `explain_query(sql)` | Return the MySQL `EXPLAIN` plan for a `SELECT` query |
| `get_table_status(database?)` | Return table status metadata |

## Example prompts

```text
List tables in the current MySQL database.
```

```text
Describe the users table, then insert 3 test users.
```

```text
Query the first 10 rows from users and summarize them.
```

## Safety behavior

- Read queries must be a single `SELECT` statement; trailing semicolons are accepted, but multi-statement SQL is rejected.
- Write operations are limited to a single `INSERT` or `UPDATE` statement.
- Dangerous operations such as `DELETE`, `DROP`, `ALTER`, `CREATE`, `TRUNCATE`, permission changes, `LOAD`, stored procedure calls, `OUTFILE`, `DUMPFILE`, and `LOAD_FILE` are rejected.
- Database and table names must be simple identifiers such as `users` or `mcp_demo`; they are validated and quoted before use.
- Use a database account with least privileges for production-like environments.
