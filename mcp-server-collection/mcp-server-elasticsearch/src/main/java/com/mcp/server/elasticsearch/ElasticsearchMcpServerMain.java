package com.mcp.server.elasticsearch;

import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import com.mcp.transport.StdioTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mcp.annotation.McpAnnotationScanner;

/**
 * Elasticsearch MCP Server 启动类
 *
 * 环境变量配置：
 * - ES_HOST：Elasticsearch 主机（默认 localhost）
 * - ES_PORT：端口号（默认 9200）
 * - ES_SCHEME：协议（默认 http）
 * - ES_USERNAME：用户名（可选）
 * - ES_PASSWORD：密码（可选）
 *
 * @author MCP Java SDK
 */
public class ElasticsearchMcpServerMain {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchMcpServerMain.class);

    public static void main(String[] args) throws Exception {
        // 从环境变量读取配置
        String host = getEnvOrDefault("ES_HOST", "localhost");
        int port = Integer.parseInt(getEnvOrDefault("ES_PORT", "9200"));
        String scheme = getEnvOrDefault("ES_SCHEME", "http");
        String username = getEnvOrDefault("ES_USERNAME", null);
        String password = getEnvOrDefault("ES_PASSWORD", null);

        log.info("Starting Elasticsearch MCP Server...");
        log.info("Connecting to Elasticsearch at {}://{}:{}", scheme, host, port);

        // 创建 Elasticsearch Server
        ElasticsearchMcpServer esServer = new ElasticsearchMcpServer(host, port, scheme, username, password);

        // 创建 MCP Server
        McpServer server = DefaultMcpServer.builder()
                .name("elasticsearch-server")
                .version("1.0.0")
                .build();

        // 通过注解自动注册工具
        McpAnnotationScanner.scan(server, esServer);

        // 启动服务
        server.start(new StdioTransport());

        log.info("Elasticsearch MCP Server started. Waiting for connections...");

        // 保持运行
        Thread.currentThread().join();
    }

    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }
}
