package com.mcp.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MCP 服务端配置属性类。
 * <p>
 * 通过 Spring Boot 的 {@code @ConfigurationProperties} 机制，将 {@code application.yml} 或
 * {@code application.properties} 中以 {@code mcp} 为前缀的配置项自动绑定到本类的字段上。
 * </p>
 * <p>
 * 配置示例：
 * <pre>
 * mcp:
 *   name: my-mcp-server
 *   version: 1.0.0
 *   transport: STDIO
 *   sse-port: 8080
 *   sse-path: /mcp
 * </pre>
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "mcp")
public class McpProperties {

    /**
     * MCP 服务端名称，用于在协议握手中标识服务端身份。
     * 默认值为 {@code "mcp-server"}。
     */
    private String name = "mcp-server";

    /**
     * MCP 服务端版本号，遵循语义化版本规范。
     * 默认值为 {@code "1.0.0"}。
     */
    private String version = "1.0.0";

    /**
     * 通信传输类型，决定 MCP 客户端与服务端之间的数据传输方式。
     * <ul>
     *   <li>{@link TransportType#STDIO} - 通过标准输入/输出进行通信（默认）</li>
     *   <li>{@link TransportType#SSE} - 通过 Server-Sent Events 进行通信</li>
     * </ul>
     */
    private TransportType transport = TransportType.STDIO;

    /**
     * SSE 传输模式下服务端监听的端口号。
     * 仅当 {@link #transport} 为 {@code SSE} 时生效，默认值为 {@code 8080}。
     */
    private int ssePort = 8080;

    /**
     * SSE 传输模式下的 HTTP 端点路径。
     * 仅当 {@link #transport} 为 {@code SSE} 时生效，默认值为 {@code "/mcp"}。
     */
    private String ssePath = "/mcp";

    /**
     * 传输类型枚举，定义 MCP 服务端支持的通信方式。
     */
    public enum TransportType {
        /** 标准输入/输出传输，适用于命令行集成场景 */
        STDIO,
        /** Server-Sent Events 传输，适用于 HTTP 长连接场景 */
        SSE
    }
}
