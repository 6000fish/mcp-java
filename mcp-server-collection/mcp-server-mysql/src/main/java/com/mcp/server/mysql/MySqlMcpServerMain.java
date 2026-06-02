package com.mcp.server.mysql;

import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import com.mcp.transport.StdioTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mcp.annotation.McpAnnotationScanner;

/**
 * MySQL MCP Server 启动类。
 * <p>
 * 作为独立进程启动 MySQL MCP Server，通过标准输入/输出（STDIO）传输方式
 * 与 MCP 客户端进行通信。
 * </p>
 * <p>
 * 启动时从环境变量读取数据库连接配置：
 * <ul>
 *   <li>{@code MYSQL_HOST} - 数据库地址，默认 localhost</li>
 *   <li>{@code MYSQL_PORT} - 数据库端口，默认 3306</li>
 *   <li>{@code MYSQL_DATABASE} - 目标数据库名，默认 test</li>
 *   <li>{@code MYSQL_USERNAME} - 用户名，默认 root</li>
 *   <li>{@code MYSQL_PASSWORD} - 密码，默认空</li>
 * </ul>
 * </p>
 */
public class MySqlMcpServerMain {

    private static final Logger log = LoggerFactory.getLogger(MySqlMcpServerMain.class);

    /**
     * 程序入口。
     * <p>
     * 初始化 MySQL 连接、创建 MCP Server 实例、注册所有数据库工具、
     * 启动 STDIO 传输层，并注册 JVM 关闭钩子以确保连接池被正确释放。
     * </p>
     *
     * @param args 命令行参数（未使用）
     * @throws Exception 启动过程中可能抛出的异常
     */
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

        // 通过注解自动注册工具
        McpAnnotationScanner.scan(server, mySqlServer);

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

    /**
     * 从环境变量中获取配置值，若不存在则返回默认值。
     *
     * @param name         环境变量名称
     * @param defaultValue 默认值
     * @return 环境变量的值，若未设置则返回 {@code defaultValue}
     */
    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }
}
