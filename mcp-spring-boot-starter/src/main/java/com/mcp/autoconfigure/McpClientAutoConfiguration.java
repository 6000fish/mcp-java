package com.mcp.autoconfigure;

import com.mcp.client.DefaultMcpClient;
import com.mcp.client.McpClient;
import com.mcp.transport.SseClientTransport;
import com.mcp.transport.StdioTransport;
import com.mcp.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * MCP 客户端自动配置类。
 * <p>
 * 基于 Spring Boot 的自动配置机制，在类路径中存在 {@link McpClient} 时自动生效。
 * 负责完成以下工作：
 * <ul>
 *   <li>读取 {@link McpClientProperties} 中的配置信息</li>
 *   <li>创建并注册 {@link McpClient} 实例（Bean）</li>
 *   <li>根据配置创建对应的 {@link Transport} 传输层实现</li>
 * </ul>
 * </p>
 */
@AutoConfiguration
@ConditionalOnClass(McpClient.class)
@EnableConfigurationProperties(McpClientProperties.class)
public class McpClientAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(McpClientAutoConfiguration.class);

    /**
     * 创建 MCP 客户端 Bean。
     * <p>
     * 使用 {@link McpClientProperties} 中的名称、版本和超时时间构建 {@link DefaultMcpClient} 实例。
     * 如果容器中已存在 {@link McpClient} 类型的 Bean，则不会重复创建。
     * </p>
     *
     * @param properties MCP 客户端配置属性
     * @return 构建完成的 MCP 客户端实例
     */
    @Bean
    @ConditionalOnMissingBean
    public McpClient mcpClient(McpClientProperties properties) {
        DefaultMcpClient client = DefaultMcpClient.builder()
                .name(properties.getName())
                .version(properties.getVersion())
                .timeout(properties.getTimeout())
                .build();
        return client;
    }

    /**
     * 创建 MCP 客户端传输层 Bean。
     * <p>
     * 根据 {@link McpClientProperties#getTransport()} 配置选择传输实现：
     * <ul>
     *   <li>{@code STDIO} - 标准输入/输出传输</li>
     *   <li>{@code SSE} - Server-Sent Events 客户端传输</li>
     * </ul>
     * </p>
     *
     * @param properties MCP 客户端配置属性
     * @return 传输层实例
     */
    @Bean
    @ConditionalOnMissingBean(name = "mcpClientTransport")
    public Transport mcpClientTransport(McpClientProperties properties) {
        return switch (properties.getTransport()) {
            case STDIO -> new StdioTransport();
            case SSE -> {
                log.info("Creating SSE client transport, url: {}", properties.getSseUrl());
                yield new SseClientTransport(properties.getSseUrl());
            }
        };
    }
}
