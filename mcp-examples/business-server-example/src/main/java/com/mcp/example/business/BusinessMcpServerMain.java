package com.mcp.example.business;

import com.mcp.annotation.McpAnnotationScanner;
import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import com.mcp.transport.StdioTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusinessMcpServerMain {

    private static final Logger log = LoggerFactory.getLogger(BusinessMcpServerMain.class);

    public static void main(String[] args) throws Exception {
        log.info("Starting Business MCP Server example...");

        BusinessMcpServer businessServer = new BusinessMcpServer();
        McpServer server = DefaultMcpServer.builder()
                .name("business-server-example")
                .version("1.0.0")
                .build();

        McpAnnotationScanner.scan(server, businessServer);
        server.start(new StdioTransport());

        log.info("Business MCP Server example started. Waiting for MCP messages on stdin...");
        Thread.currentThread().join();
    }
}
