package com.mcp.example.template;

import com.mcp.annotation.McpAnnotationScanner;
import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import com.mcp.transport.StdioTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomServerTemplateMain {

    private static final Logger log = LoggerFactory.getLogger(CustomServerTemplateMain.class);

    public static void main(String[] args) throws Exception {
        log.info("Starting custom MCP server template...");

        CustomMcpServer customServer = new CustomMcpServer();
        McpServer server = DefaultMcpServer.builder()
                .name("custom-server-template")
                .version("1.0.0")
                .build();

        McpAnnotationScanner.scan(server, customServer);
        server.start(new StdioTransport());

        log.info("Custom MCP server template started. Waiting for MCP messages on stdin...");
        Thread.currentThread().join();
    }
}
