package com.mcp.server.postgresql;

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
 * PostgreSQL MCP Server 实现
 *
 * 提供 PostgreSQL 数据库操作的 MCP 工具，包括：
 * - query：执行 SELECT 查询
 * - execute：执行 INSERT/UPDATE/DELETE 语句
 * - list_databases：列出所有数据库
 * - list_tables：列出当前数据库的所有表
 * - describe_table：查看表结构
 * - explain_query：查看查询执行计划
 * - get_table_stats：获取表统计信息
 *
 * 安全特性：
 * - SQL 安全检查，阻止危险操作（DROP、DELETE、TRUNCATE 等）
 * - 使用 HikariCP 连接池管理数据库连接
 *
 * @author MCP Java SDK
 */
@McpServer(name = "postgresql-server", version = "1.0.0")
public class PostgreSqlMcpServer {

    private static final Logger log = LoggerFactory.getLogger(PostgreSqlMcpServer.class);

    /** 数据库连接池 */
    private final HikariDataSource dataSource;

    /**
     * 创建 PostgreSQL MCP Server
     *
     * @param host     数据库主机
     * @param port     数据库端口
     * @param database 数据库名称
     * @param username 用户名
     * @param password 密码
     */
    public PostgreSqlMcpServer(String host, int port, String database, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(10000);

        this.dataSource = new HikariDataSource(config);
        log.info("PostgreSQL connection pool initialized for {}:{}", host, port);
    }

    /**
     * 执行 SQL 查询
     *
     * 执行 SELECT 查询并返回结果集，支持所有标准 SQL 查询语法。
     * 危险 SQL 语句（DROP、DELETE、TRUNCATE 等）会被阻止执行。
     *
     * @param sql 要执行的 SQL 查询语句
     * @return 查询结果，以 JSON 格式返回
     */
    @McpTool(name = "query", description = "Execute a SQL query and return results")
    public ToolCallResult query(
            @Param(name = "sql", description = "SQL query to execute") String sql
    ) {
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
     * 执行 SQL 语句
     *
     * 执行 INSERT、UPDATE、DELETE 等非查询语句，返回受影响的行数。
     * 危险 SQL 语句会被阻止执行。
     *
     * @param sql 要执行的 SQL 语句
     * @return 执行结果，包含受影响行数
     */
    @McpTool(name = "execute", description = "Execute a SQL statement (INSERT, UPDATE, DELETE)")
    public ToolCallResult execute(
            @Param(name = "sql", description = "SQL statement to execute") String sql
    ) {
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
     * 列出所有数据库
     *
     * 查询 pg_database 系统目录获取所有数据库列表。
     *
     * @return 数据库名称列表
     */
    @McpTool(name = "list_databases", description = "List all databases")
    public ToolCallResult listDatabases() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT datname FROM pg_database WHERE datistemplate = false ORDER BY datname")) {

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
     * 列出当前数据库的所有表
     *
     * 查询 pg_tables 系统目录获取表列表，可指定 schema。
     *
     * @param schema 数据库 schema 名称（可选，默认为 public）
     * @return 表名列表
     */
    @McpTool(name = "list_tables", description = "List all tables in current database")
    public ToolCallResult listTables(
            @Param(name = "schema", description = "Schema name (default: public)", required = false) String schema
    ) {
        String schemaName = schema != null ? schema : "public";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT tablename FROM pg_tables WHERE schemaname = '" + schemaName + "' ORDER BY tablename")) {

            List<String> tables = new ArrayList<>();
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
            return ToolCallResult.json(tables);

        } catch (SQLException e) {
            return ToolCallResult.error("Failed to list tables: " + e.getMessage());
        }
    }

    /**
     * 查看表结构
     *
     * 查询 information_schema.columns 获取表的列信息，包括列名、数据类型、是否可空等。
     *
     * @param table  表名
     * @param schema 数据库 schema 名称（可选，默认为 public）
     * @return 表结构信息
     */
    @McpTool(name = "describe_table", description = "Describe table structure")
    public ToolCallResult describeTable(
            @Param(name = "table", description = "Table name") String table,
            @Param(name = "schema", description = "Schema name (default: public)", required = false) String schema
    ) {
        String schemaName = schema != null ? schema : "public";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT column_name, data_type, is_nullable, column_default " +
                     "FROM information_schema.columns " +
                     "WHERE table_schema = '" + schemaName + "' AND table_name = '" + table + "' " +
                     "ORDER BY ordinal_position")) {

            List<Map<String, Object>> columns = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> column = new LinkedHashMap<>();
                column.put("column_name", rs.getString("column_name"));
                column.put("data_type", rs.getString("data_type"));
                column.put("is_nullable", rs.getString("is_nullable"));
                column.put("column_default", rs.getString("column_default"));
                columns.add(column);
            }
            return ToolCallResult.json(columns);

        } catch (SQLException e) {
            return ToolCallResult.error("Failed to describe table: " + e.getMessage());
        }
    }

    /**
     * 查看查询执行计划
     *
     * 使用 EXPLAIN ANALYZE 获取查询的执行计划和性能统计信息。
     *
     * @param sql 要分析的 SQL 查询
     * @return 执行计划详情
     */
    @McpTool(name = "explain_query", description = "Get execution plan for a SQL query")
    public ToolCallResult explainQuery(
            @Param(name = "sql", description = "SQL query to explain") String sql
    ) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("EXPLAIN ANALYZE " + sql)) {

            List<String> plan = new ArrayList<>();
            while (rs.next()) {
                plan.add(rs.getString(1));
            }
            return ToolCallResult.json(plan);

        } catch (SQLException e) {
            return ToolCallResult.error("Failed to explain query: " + e.getMessage());
        }
    }

    /**
     * 获取表统计信息
     *
     * 查询 pg_stat_user_tables 获取表的行数、扫描次数等统计信息。
     *
     * @param schema 数据库 schema 名称（可选，默认为 public）
     * @return 表统计信息
     */
    @McpTool(name = "get_table_stats", description = "Get table statistics")
    public ToolCallResult getTableStats(
            @Param(name = "schema", description = "Schema name (default: public)", required = false) String schema
    ) {
        String schemaName = schema != null ? schema : "public";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT relname, n_live_tup, n_dead_tup, seq_scan, idx_scan " +
                     "FROM pg_stat_user_tables " +
                     "WHERE schemaname = '" + schemaName + "' " +
                     "ORDER BY n_live_tup DESC")) {

            List<Map<String, Object>> stats = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> stat = new LinkedHashMap<>();
                stat.put("table_name", rs.getString("relname"));
                stat.put("live_tuples", rs.getLong("n_live_tup"));
                stat.put("dead_tuples", rs.getLong("n_dead_tup"));
                stat.put("seq_scans", rs.getLong("seq_scan"));
                stat.put("idx_scans", rs.getLong("idx_scan"));
                stats.add(stat);
            }
            return ToolCallResult.json(stats);

        } catch (SQLException e) {
            return ToolCallResult.error("Failed to get table stats: " + e.getMessage());
        }
    }

    /**
     * SQL 安全检查
     *
     * 检查 SQL 语句是否包含危险操作，包括：
     * - DROP：删除表/数据库
     * - DELETE：删除数据
     * - TRUNCATE：清空表
     * - ALTER：修改表结构
     * - CREATE：创建表/数据库
     * - GRANT：授权
     * - REVOKE：撤销权限
     *
     * @param sql SQL 语句
     * @return true 表示危险，false 表示安全
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
     * 关闭数据库连接池
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("PostgreSQL connection pool closed");
        }
    }
}
