# MySQL MCP Server 使用手册

中文 | [English](../mysql-server.md)

MySQL MCP Server 通过 stdio 将数据库发现、安全查询和受控数据修改暴露为 MCP 工具。

## 构建

```bash
mvn package -pl mcp-server-collection/mcp-server-mysql -am -DskipTests
```

可执行 jar 生成位置：

```text
mcp-server-collection/mcp-server-mysql/target/mcp-server-mysql-0.1.0.jar
```

## 配置

环境变量：

| 变量 | 必填 | 默认值 | 说明 |
|---|---:|---|---|
| `MYSQL_HOST` | 否 | `localhost` | MySQL 地址 |
| `MYSQL_PORT` | 否 | `3306` | MySQL 端口 |
| `MYSQL_DATABASE` | 否 | `test` | 默认数据库 |
| `MYSQL_USERNAME` | 否 | `root` | MySQL 用户名 |
| `MYSQL_PASSWORD` | 否 | 空 | MySQL 密码 |

## Agent 配置

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

## 示例数据库

```sql
CREATE DATABASE IF NOT EXISTS mcp_demo;
USE mcp_demo;
CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL,
  age INT NOT NULL
);
```

## 工具

| 工具 | 说明 |
|---|---|
| `query` | 执行只读 `SELECT` 查询，并以 JSON 返回数据行 |
| `execute` | 执行安全的 `INSERT` 或 `UPDATE` 语句 |
| `list_databases` | 列出可见数据库 |
| `list_tables` | 列出当前或指定数据库中的表 |
| `describe_table` | 查看表字段和类型 |
| `explain_query` | 返回 `SELECT` 查询的 MySQL `EXPLAIN` 执行计划 |
| `get_table_status` | 返回表状态元数据 |

## 示例 Prompt

```text
List tables in the current MySQL database.
```

```text
Describe the users table, then insert 3 test users.
```

```text
Query the first 10 rows from users and summarize them.
```

## 安全行为

- 读查询必须是单条 `SELECT` 语句。
- 写操作仅允许 `INSERT` 和 `UPDATE`。
- 拒绝 `DELETE`、`DROP`、`ALTER`、`TRUNCATE`、权限变更和多语句 SQL 等危险操作。
- 在类生产环境中建议使用最小权限数据库账号。
