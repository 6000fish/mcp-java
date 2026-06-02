package com.mcp.server.mysql;

import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import com.mcp.transport.StdioTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MySQL MCP Server 启动类
 */
public class MySqlMcpServerMain {

    private static final Logger log = LoggerFactory.getLogger(MySqlMcpServerMain.class);

    public static void main(String[] args) throws Exception {
        // 从环境变量读取配置
        String host = getEnvOrDefault("MYSQL_HOST", "localhost");
        int port = Integer.parseInt(getEnvOrDefault("MYSQL_PORT", "3306"));
        String database = getEnvOrDefault("MYSQL_DATABASE", "test");
        String username = getEnvOrDefault("MYSQL_USERNAME", "root");
        String password = getEnvOrDefault("MYSQL_PASSWORD", "");

        log.info("Starting MySQL MCP Server...");
        log.info("Connecting to MySQL at {}:{}", host, port);

        // 创建 MySQL Server
        MySqlMcpServer mySqlServer = new MySqlMcpServer(host, port, database, username, password);

        // 创建 MCP Server
        McpServer server = DefaultMcpServer.builder()
                .name("mysql-server")
                .version("1.0.0")
                .build();

        // 注册工具
        server.tool("query", "Execute a SQL query", params ->
                mySqlServer.query((String) params.get("sql")));
        server.tool("execute", "Execute a SQL statement", params ->
                mySqlServer.execute((String) params.get("sql")));
        server.tool("list_databases", "List all databases", params ->
                mySqlServer.listDatabases());
        server.tool("list_tables", "List all tables", params ->
                mySqlServer.listTables((String) params.get("database")));
        server.tool("describe_table", "Describe table structure", params ->
                mySqlServer.describeTable(
                        (String) params.get("table"),
                        (String) params.get("database")));
        server.tool("explain_query", "Get execution plan", params ->
                mySqlServer.explainQuery((String) params.get("sql")));
        server.tool("get_table_status", "Get table status", params ->
                mySqlServer.getTableStatus((String) params.get("database")));

        // 启动服务
        server.start(new StdioTransport());

        log.info("MySQL MCP Server started. Waiting for connections...");

        // 保持运行
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down MySQL MCP Server...");
            mySqlServer.close();
        }));

        // 阻塞主线程
        Thread.currentThread().join();
    }

    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }
}
