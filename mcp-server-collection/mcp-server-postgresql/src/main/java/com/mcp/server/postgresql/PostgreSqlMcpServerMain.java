package com.mcp.server.postgresql;

import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import com.mcp.transport.StdioTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mcp.annotation.McpAnnotationScanner;

/**
 * PostgreSQL MCP Server 启动类
 *
 * 环境变量配置：
 * - PG_HOST：数据库主机（默认 localhost）
 * - PG_PORT：数据库端口（默认 5432）
 * - PG_DATABASE：数据库名称（默认 postgres）
 * - PG_USERNAME：用户名（默认 postgres）
 * - PG_PASSWORD：密码（默认空）
 *
 * @author MCP Java SDK
 */
public class PostgreSqlMcpServerMain {

    private static final Logger log = LoggerFactory.getLogger(PostgreSqlMcpServerMain.class);

    public static void main(String[] args) throws Exception {
        // 从环境变量读取配置
        String host = getEnvOrDefault("PG_HOST", "localhost");
        int port = Integer.parseInt(getEnvOrDefault("PG_PORT", "5432"));
        String database = getEnvOrDefault("PG_DATABASE", "postgres");
        String username = getEnvOrDefault("PG_USERNAME", "postgres");
        String password = getEnvOrDefault("PG_PASSWORD", "");

        log.info("Starting PostgreSQL MCP Server...");
        log.info("Connecting to PostgreSQL at {}:{}", host, port);

        // 创建 PostgreSQL Server
        PostgreSqlMcpServer pgServer = new PostgreSqlMcpServer(host, port, database, username, password);

        // 创建 MCP Server
        McpServer server = DefaultMcpServer.builder()
                .name("postgresql-server")
                .version("1.0.0")
                .build();

        // 通过注解自动注册工具
        McpAnnotationScanner.scan(server, pgServer);

        // 启动服务
        server.start(new StdioTransport());

        log.info("PostgreSQL MCP Server started. Waiting for connections...");

        // 保持运行
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down PostgreSQL MCP Server...");
            pgServer.close();
        }));

        // 阻塞主线程
        Thread.currentThread().join();
    }

    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }
}
