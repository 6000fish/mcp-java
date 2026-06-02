package com.mcp.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MCP 配置属性
 */
@Data
@ConfigurationProperties(prefix = "mcp")
public class McpProperties {

    /**
     * Server 名称
     */
    private String name = "mcp-server";

    /**
     * Server 版本
     */
    private String version = "1.0.0";

    /**
     * 传输类型
     */
    private TransportType transport = TransportType.STDIO;

    /**
     * SSE 端口（仅 SSE 传输类型有效）
     */
    private int ssePort = 8080;

    /**
     * SSE 路径（仅 SSE 传输类型有效）
     */
    private String ssePath = "/mcp";

    public enum TransportType {
        STDIO,
        SSE
    }
}
