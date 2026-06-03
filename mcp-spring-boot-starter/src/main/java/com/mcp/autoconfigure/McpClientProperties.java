package com.mcp.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MCP 客户端配置属性类。
 * <p>
 * 通过 Spring Boot 的 {@code @ConfigurationProperties} 机制，将 {@code application.yml} 或
 * {@code application.properties} 中以 {@code mcp.client} 为前缀的配置项自动绑定到本类的字段上。
 * </p>
 * <p>
 * 配置示例：
 * <pre>
 * mcp:
 *   client:
 *     name: my-mcp-client
 *     version: 1.0.0
 *     timeout: 30
 *     transport: STDIO
 *     sse-url: http://localhost:8080/sse
 * </pre>
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "mcp.client")
public class McpClientProperties {

    /**
     * MCP 客户端名称，用于在协议握手中标识客户端身份。
     * 默认值为 {@code "mcp-client"}。
     */
    private String name = "mcp-client";

    /**
     * MCP 客户端版本号，遵循语义化版本规范。
     * 默认值为 {@code "1.0.0"}。
     */
    private String version = "1.0.0";

    /**
     * 请求超时时间（秒），超过该时间未收到响应将抛出超时异常。
     * 默认值为 {@code 30} 秒。
     */
    private long timeout = 30;

    /**
     * 通信传输类型，决定 MCP 客户端与服务端之间的数据传输方式。
     * <ul>
     *   <li>{@link ClientTransportType#STDIO} - 通过标准输入/输出进行通信（默认）</li>
     *   <li>{@link ClientTransportType#SSE} - 通过 Server-Sent Events 进行通信</li>
     * </ul>
     */
    private ClientTransportType transport = ClientTransportType.STDIO;

    /**
     * SSE 传输模式下服务端的 URL 地址。
     * 仅当 {@link #transport} 为 {@code SSE} 时生效，默认值为 {@code "http://localhost:8080/sse"}。
     */
    private String sseUrl = "http://localhost:8080/sse";

    /**
     * 客户端传输类型枚举，定义 MCP 客户端支持的通信方式。
     */
    public enum ClientTransportType {
        /** 标准输入/输出传输，适用于命令行集成场景 */
        STDIO,
        /** Server-Sent Events 传输，适用于 HTTP 长连接场景 */
        SSE
    }
}
