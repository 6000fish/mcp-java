package com.mcp.autoconfigure;

import com.mcp.annotation.McpAnnotationScanner;
import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import com.mcp.transport.SseTransport;
import com.mcp.transport.StdioTransport;
import com.mcp.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.Map;

/**
 * MCP 自动配置类。
 * <p>
 * 基于 Spring Boot 的自动配置机制，在类路径中存在 {@link McpServer} 时自动生效。
 * 负责完成以下工作：
 * <ul>
 *   <li>读取 {@link McpProperties} 中的配置信息</li>
 *   <li>创建并注册 {@link McpServer} 实例（Bean）</li>
 *   <li>扫描 Spring 容器中带有 {@code @McpServer} 注解的 Bean，自动注册其中的 MCP 工具、资源和提示</li>
 *   <li>根据配置创建对应的 {@link Transport} 传输层实现</li>
 * </ul>
 * </p>
 */
@AutoConfiguration
@ConditionalOnClass(McpServer.class)
@EnableConfigurationProperties(McpProperties.class)
public class McpAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(McpAutoConfiguration.class);

    /**
     * 创建 MCP 服务端 Bean。
     * <p>
     * 使用 {@link McpProperties} 中的名称和版本构建 {@link DefaultMcpServer} 实例，
     * 并自动扫描 Spring 容器中所有带 {@code @McpServer} 注解的 Bean，
     * 将其内部标注了 {@code @McpTool}、{@code @McpResource}、{@code @McpPrompt} 的方法
     * 注册为对应的 MCP 协议元素。
     * </p>
     *
     * @param properties MCP 配置属性
     * @param context    Spring 应用上下文，用于扫描注解 Bean；可选，为空时跳过自动注册
     * @return 构建完成的 MCP 服务端实例
     */
    @Bean
    @ConditionalOnMissingBean
    public McpServer mcpServer(McpProperties properties,
                               @Autowired(required = false) ApplicationContext context) {
        DefaultMcpServer server = DefaultMcpServer.builder()
                .name(properties.getName())
                .version(properties.getVersion())
                .build();

        if (context != null) {
            Map<String, Object> beans = context.getBeansWithAnnotation(com.mcp.annotation.McpServer.class);
            for (Object bean : beans.values()) {
                McpAnnotationScanner.scan(server, bean);
            }
        }

        return server;
    }

    /**
     * 创建 MCP 传输层 Bean。
     * <p>
     * 根据 {@link McpProperties#getTransport()} 配置选择传输实现：
     * <ul>
     *   <li>{@code STDIO} - 标准输入/输出传输</li>
     *   <li>{@code SSE} - Server-Sent Events 传输（暂未实现，回退到 STDIO）</li>
     * </ul>
     * </p>
     *
     * @param properties MCP 配置属性
     * @return 传输层实例
     */
    @Bean
    @ConditionalOnMissingBean
    public Transport mcpTransport(McpProperties properties) {
        return switch (properties.getTransport()) {
            case STDIO -> new StdioTransport();
            case SSE -> {
                log.info("Creating SSE transport on port {}, path {}",
                        properties.getSsePort(), properties.getSsePath());
                yield new SseTransport(properties.getSsePort(), properties.getSsePath(), "/messages");
            }
        };
    }

    /**
     * 启动 MCP Server，将 Server 和 Transport 连接起来。
     * <p>
     * 在 Spring 容器初始化完成后，自动调用 {@link McpServer#start(Transport)} 启动服务。
     * 对于 STDIO 模式，开始监听 stdin；对于 SSE 模式，启动 HTTP 服务器。
     * </p>
     *
     * @param server    MCP 服务端实例
     * @param transport 传输层实例
     * @return 应用启动器
     */
    @Bean
    public ApplicationRunner mcpServerRunner(McpServer server, Transport transport) {
        return args -> {
            log.info("Starting MCP Server with {} transport...", transport.getClass().getSimpleName());
            server.start(transport);
        };
    }
}
