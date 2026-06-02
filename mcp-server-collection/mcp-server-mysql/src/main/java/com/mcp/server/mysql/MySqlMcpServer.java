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
 * MySQL MCP Server 实现
 */
@McpServer(name = "mysql-server", version = "1.0.0")
public class MySqlMcpServer {

    private static final Logger log = LoggerFactory.getLogger(MySqlMcpServer.class);

    private final HikariDataSource dataSource;

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

    @McpTool(name = "query", description = "Execute a SQL query and return results")
    public ToolCallResult query(
            @Param(name = "sql", description = "SQL query to execute") String sql
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

    @McpTool(name = "execute", description = "Execute a SQL statement (INSERT, UPDATE, DELETE)")
    public ToolCallResult execute(
            @Param(name = "sql", description = "SQL statement to execute") String sql
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

    @McpTool(name = "list_databases", description = "List all databases")
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

    @McpTool(name = "list_tables", description = "List all tables in a database")
    public ToolCallResult listTables(
            @Param(name = "database", description = "Database name", required = false) String database
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

    @McpTool(name = "describe_table", description = "Describe table structure")
    public ToolCallResult describeTable(
            @Param(name = "table", description = "Table name") String table,
            @Param(name = "database", description = "Database name", required = false) String database
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

    @McpTool(name = "explain_query", description = "Get execution plan for a SQL query")
    public ToolCallResult explainQuery(
            @Param(name = "sql", description = "SQL query to explain") String sql
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

    @McpTool(name = "get_table_status", description = "Get table status information")
    public ToolCallResult getTableStatus(
            @Param(name = "database", description = "Database name", required = false) String database
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

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("MySQL connection pool closed");
        }
    }
}
