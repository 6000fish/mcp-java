package com.mcp.server.docker;

import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import com.mcp.transport.StdioTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mcp.annotation.McpAnnotationScanner;

/**
 * Docker MCP Server 启动类
 *
 * 环境变量配置：
 * - DOCKER_HOST：Docker 守护进程地址（可选，默认使用本地 socket）
 *
 * @author MCP Java SDK
 */
public class DockerMcpServerMain {

    private static final Logger log = LoggerFactory.getLogger(DockerMcpServerMain.class);

    public static void main(String[] args) throws Exception {
        log.info("Starting Docker MCP Server...");

        // 创建 Docker Server
        DockerMcpServer dockerServer = new DockerMcpServer();

        // 创建 MCP Server
        McpServer server = DefaultMcpServer.builder()
                .name("docker-server")
                .version("1.0.0")
                .build();

        // 通过注解自动注册工具
        McpAnnotationScanner.scan(server, dockerServer);

        // 启动服务
        server.start(new StdioTransport());

        log.info("Docker MCP Server started. Waiting for connections...");

        // 保持运行
        Thread.currentThread().join();
    }
}
