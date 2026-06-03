package com.mcp.server.mysql;

import com.mcp.annotation.McpServer;
import com.mcp.annotation.McpTool;
import com.mcp.annotation.Param;
import com.mcp.protocol.ToolCallResult;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * MySQL MCP Server 实现。
 * <p>
 * 本类通过 MCP（Model Context Protocol）协议将 MySQL 数据库操作暴露为可调用的工具，
 * 使 AI 模型能够直接与 MySQL 数据库进行交互。
 * </p>
 * <p>
 * 提供以下 MCP 工具：
 * <ul>
 *   <li><b>query</b> - 执行 SELECT 查询并返回结果集</li>
 *   <li><b>execute</b> - 执行 INSERT/UPDATE/DELETE 等写操作</li>
 *   <li><b>list_databases</b> - 列出所有数据库</li>
 *   <li><b>list_tables</b> - 列出指定数据库中的所有表</li>
 *   <li><b>describe_table</b> - 查看表结构</li>
 *   <li><b>explain_query</b> - 获取 SQL 查询的执行计划</li>
 *   <li><b>get_table_status</b> - 获取表的状态信息</li>
 * </ul>
 * </p>
 * <p>
 * 安全特性：内置危险 SQL 检测机制，自动拦截 DROP、DELETE、TRUNCATE、ALTER、
 * CREATE、GRANT、REVOKE 等可能造成数据损失的操作。
 * </p>
 */
@McpServer(name = "mysql-server", version = "1.0.0")
public class MySqlMcpServer {

    private static final Logger log = LoggerFactory.getLogger(MySqlMcpServer.class);

    /** HikariCP 数据库连接池，管理与 MySQL 的连接 */
    private final HikariDataSource dataSource;

    /**
     * 构造方法，初始化 MySQL 连接池。
     * <p>
     * 使用 HikariCP 连接池，配置最大连接数为 10，最小空闲连接数为 2，
     * 空闲超时 30 秒，连接超时 10 秒。
     * </p>
     *
     * @param host     MySQL 服务器地址
     * @param port     MySQL 服务器端口
     * @param database 目标数据库名称
     * @param username 数据库用户名
     * @param password 数据库密码
     */
    public MySqlMcpServer(String host, int port, String database, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true",
                host, port, database));
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(10000);

        this.dataSource = new HikariDataSource(config);
        log.info("MySQL connection pool initialized for {}:{}", host, port);
    }

    /**
     * 执行 SQL 查询语句并返回结果集。
     * <p>
     * 仅允许执行 SELECT 等只读查询。执行前会进行危险 SQL 检测，
     * 拒绝任何以 DROP、DELETE、TRUNCATE 等关键字开头的语句。
     * </p>
     *
     * @param sql 要执行的 SQL 查询语句
     * @return 查询结果，以 JSON 数组格式返回，每个元素为一行数据（列名 -> 值的映射）；
     *         若 SQL 被判定为危险操作则返回错误信息
     */
    @McpTool(name = "query", description = "Execute a read-only MySQL SELECT query and return rows as JSON. Use this for data lookup, filtering, aggregation, and reporting; do not use it for INSERT, UPDATE, DELETE, or schema changes.")
    public ToolCallResult query(
            @Param(name = "sql", description = "Read-only SELECT SQL to execute, for example: SELECT name, age FROM users WHERE id = 1") String sql
    ) {
        // 安全检查
        if (isDangerousSql(sql)) {
            return ToolCallResult.error("Dangerous SQL operations are not allowed");
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<Map<String, Object>> results = new ArrayList<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnLabel(i), rs.getObject(i));
                }
                results.add(row);
            }

            return ToolCallResult.json(results);

        } catch (SQLException e) {
            log.error("Query execution failed", e);
            return ToolCallResult.error("Query failed: " + e.getMessage());
        }
    }

    /**
     * 执行 SQL 写操作语句（INSERT、UPDATE、DELETE 等）。
     * <p>
     * 执行前会进行危险 SQL 检测，拒绝可能造成数据结构变更或数据大量损失的操作。
     * </p>
     *
     * @param sql 要执行的 SQL 语句
     * @return 执行结果，包含受影响的行数；若 SQL 被判定为危险操作则返回错误信息
     */
    @McpTool(name = "execute", description = "Execute a MySQL data modification statement such as INSERT or UPDATE and return the affected row count. Use query for SELECT statements. Dangerous schema or permission operations are rejected.")
    public ToolCallResult execute(
            @Param(name = "sql", description = "Data modification SQL to execute, for example: INSERT INTO users(name, age) VALUES ('Alice', 28)") String sql
    ) {
        // 安全检查
        if (isDangerousSql(sql)) {
            return ToolCallResult.error("Dangerous SQL operations are not allowed");
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            int affectedRows = stmt.executeUpdate(sql);
            return ToolCallResult.success("Statement executed. Rows affected: " + affectedRows);

        } catch (SQLException e) {
            log.error("Statement execution failed", e);
            return ToolCallResult.error("Execution failed: " + e.getMessage());
        }
    }

    /**
     * 列出 MySQL 服务器中的所有数据库。
     * <p>等效于执行 {@code SHOW DATABASES} 语句。</p>
     *
     * @return 数据库名称列表，以 JSON 数组格式返回
     */
    @McpTool(name = "list_databases", description = "List databases visible to the configured MySQL user. Use this before table discovery when the user asks what databases are available.")
    public ToolCallResult listDatabases() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW DATABASES")) {

            List<String> databases = new ArrayList<>();
            while (rs.next()) {
                databases.add(rs.getString(1));
            }
            return ToolCallResult.json(databases);

        } catch (SQLException e) {
            return ToolCallResult.error("Failed to list databases: " + e.getMessage());
        }
    }

    /**
     * 列出指定数据库中的所有表。
     * <p>
     * 若未指定数据库名称，则列出当前连接数据库中的表。
     * 等效于执行 {@code SHOW TABLES} 或 {@code SHOW TABLES FROM database}。
     * </p>
     *
     * @param database 数据库名称，可选；为空时使用当前连接的数据库
     * @return 表名列表，以 JSON 数组格式返回
     */
    @McpTool(name = "list_tables", description = "List tables in the current or specified MySQL database. Use this when the user asks what tables exist or before writing SQL against an unknown schema.")
    public ToolCallResult listTables(
            @Param(name = "database", description = "Optional database name. Leave empty to list tables in the configured default database.", required = false) String database
    ) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = database != null ?
                    "SHOW TABLES FROM " + database :
                    "SHOW TABLES";

            try (ResultSet rs = stmt.executeQuery(sql)) {
                List<String> tables = new ArrayList<>();
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
                return ToolCallResult.json(tables);
            }

        } catch (SQLException e) {
            return ToolCallResult.error("Failed to list tables: " + e.getMessage());
        }
    }

    /**
     * 查看指定表的结构信息。
     * <p>
     * 等效于执行 {@code DESCRIBE table} 语句，返回字段名称、类型、是否可为 NULL、
     * 键类型、默认值等表结构详情。
     * </p>
     *
     * @param table    表名
     * @param database 数据库名称，可选；不为空时会拼接为 {@code database.table} 格式
     * @return 表结构信息，以 JSON 数组格式返回，每个元素描述一列的属性
     */
    @McpTool(name = "describe_table", description = "Describe columns, types, nullability, keys, and defaults for a MySQL table. Use this before generating SELECT or INSERT SQL when the table structure is unknown.")
    public ToolCallResult describeTable(
            @Param(name = "table", description = "Table name to inspect, without database prefix when database is provided separately") String table,
            @Param(name = "database", description = "Optional database name. Leave empty to inspect a table in the configured default database.", required = false) String database
    ) {
        String fullTable = database != null ? database + "." + table : table;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE " + fullTable)) {

            List<Map<String, Object>> columns = new ArrayList<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> column = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    column.put(metaData.getColumnLabel(i), rs.getObject(i));
                }
                columns.add(column);
            }

            return ToolCallResult.json(columns);

        } catch (SQLException e) {
            return ToolCallResult.error("Failed to describe table: " + e.getMessage());
        }
    }

    /**
     * 获取 SQL 查询的执行计划。
     * <p>
     * 等效于执行 {@code EXPLAIN sql} 语句，用于分析查询性能、查看索引使用情况、
     * 表扫描方式等优化相关信息。
     * </p>
     *
     * @param sql 要分析的 SQL 查询语句
     * @return 执行计划详情，以 JSON 数组格式返回
     */
    @McpTool(name = "explain_query", description = "Return the MySQL EXPLAIN plan for a SELECT query. Use this to analyze performance, indexes, join order, and scan type without executing data changes.")
    public ToolCallResult explainQuery(
            @Param(name = "sql", description = "SELECT query to explain, without the EXPLAIN keyword") String sql
    ) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("EXPLAIN " + sql)) {

            List<Map<String, Object>> plan = new ArrayList<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnLabel(i), rs.getObject(i));
                }
                plan.add(row);
            }

            return ToolCallResult.json(plan);

        } catch (SQLException e) {
            return ToolCallResult.error("Failed to explain query: " + e.getMessage());
        }
    }

    /**
     * 获取表的状态信息。
     * <p>
     * 等效于执行 {@code SHOW TABLE STATUS} 语句，返回表的引擎类型、行数估计、
     * 数据长度、索引长度、自增列值等运行时状态信息。
     * </p>
     *
     * @param database 数据库名称，可选；为空时使用当前连接的数据库
     * @return 表状态信息，以 JSON 数组格式返回
     */
    @McpTool(name = "get_table_status", description = "Return MySQL table status metadata such as engine, row estimate, data length, index length, and auto-increment value for the current or specified database.")
    public ToolCallResult getTableStatus(
            @Param(name = "database", description = "Optional database name. Leave empty to inspect tables in the configured default database.", required = false) String database
    ) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = database != null ?
                    "SHOW TABLE STATUS FROM " + database :
                    "SHOW TABLE STATUS";

            try (ResultSet rs = stmt.executeQuery(sql)) {
                List<Map<String, Object>> status = new ArrayList<>();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnLabel(i), rs.getObject(i));
                    }
                    status.add(row);
                }

                return ToolCallResult.json(status);
            }

        } catch (SQLException e) {
            return ToolCallResult.error("Failed to get table status: " + e.getMessage());
        }
    }

    /**
     * 检测 SQL 语句是否为危险操作。
     * <p>
     * 判定规则：SQL 语句（去除前后空格并转为大写后）以以下关键字之一开头即视为危险：
     * DROP、DELETE、TRUNCATE、ALTER、CREATE、GRANT、REVOKE。
     * </p>
     *
     * @param sql 待检测的 SQL 语句
     * @return {@code true} 表示危险操作，{@code false} 表示安全
     */
    private boolean isDangerousSql(String sql) {
        String upper = sql.toUpperCase().trim();
        return upper.startsWith("DROP") ||
                upper.startsWith("DELETE") ||
                upper.startsWith("TRUNCATE") ||
                upper.startsWith("ALTER") ||
                upper.startsWith("CREATE") ||
                upper.startsWith("GRANT") ||
                upper.startsWith("REVOKE");
    }

    /**
     * 关闭数据库连接池，释放所有数据库连接资源。
     * <p>应在 MCP Server 停止时调用，通常通过 JVM 关闭钩子触发。</p>
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("MySQL connection pool closed");
        }
    }
}
