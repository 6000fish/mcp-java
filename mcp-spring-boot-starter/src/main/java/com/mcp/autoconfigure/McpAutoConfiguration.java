package com.mcp.autoconfigure;

import com.mcp.annotation.McpTool;
import com.mcp.protocol.ToolCallResult;
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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
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
            registerAnnotatedTools(server, context);
            registerAnnotatedResources(server, context);
            registerAnnotatedPrompts(server, context);
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

    /**
     * 扫描并注册所有带 {@code @McpTool} 注解的方法为 MCP 工具。
     * <p>
     * 遍历 Spring 容器中所有带 {@code @McpServer} 注解的 Bean，查找其中标注了
     * {@code @McpTool} 的方法，通过反射将方法参数与 MCP 工具参数进行绑定，
     * 并将方法执行结果转换为 {@link ToolCallResult} 返回。
     * </p>
     *
     * @param server  MCP 服务端实例
     * @param context Spring 应用上下文
     */
    private void registerAnnotatedTools(McpServer server, ApplicationContext context) {
        Map<String, Object> beans = context.getBeansWithAnnotation(com.mcp.annotation.McpServer.class);

        for (Object bean : beans.values()) {
            Class<?> clazz = bean.getClass();
            for (Method method : clazz.getDeclaredMethods()) {
                McpTool annotation = method.getAnnotation(McpTool.class);
                if (annotation != null) {
                    String toolName = annotation.name();
                    String description = annotation.description();

                    server.tool(toolName, description, arguments -> {
                        try {
                            Object[] args = resolveMethodArguments(method, arguments);
                            Object result = method.invoke(bean, args);

                            if (result instanceof ToolCallResult) {
                                return (ToolCallResult) result;
                            }
                            return ToolCallResult.success(String.valueOf(result));
                        } catch (Exception e) {
                            return ToolCallResult.error("Tool execution failed: " + e.getMessage());
                        }
                    });

                    log.debug("Registered tool: {}", toolName);
                }
            }
        }
    }

    /**
     * 扫描并注册所有带 {@code @McpResource} 注解的方法为 MCP 资源。
     * <p>当前尚未实现，预留接口供后续扩展。</p>
     *
     * @param server  MCP 服务端实例
     * @param context Spring 应用上下文
     */
    private void registerAnnotatedResources(McpServer server, ApplicationContext context) {
        // TODO: 实现资源注册
    }

    /**
     * 扫描并注册所有带 {@code @McpPrompt} 注解的方法为 MCP 提示模板。
     * <p>当前尚未实现，预留接口供后续扩展。</p>
     *
     * @param server  MCP 服务端实例
     * @param context Spring 应用上下文
     */
    private void registerAnnotatedPrompts(McpServer server, ApplicationContext context) {
        // TODO: 实现 Prompt 注册
    }

    /**
     * 解析 MCP 工具方法的参数，将调用参数映射到 Java 方法参数。
     * <p>
     * 参数绑定优先级：
     * <ol>
     *   <li>通过 {@code @Param} 注解的 name 属性匹配</li>
     *   <li>通过方法参数名称匹配（无注解时的回退策略）</li>
     * </ol>
     * 如果标注了 {@code required = true} 的参数缺失，将抛出 {@link IllegalArgumentException}。
     * </p>
     *
     * @param method    目标方法
     * @param arguments MCP 调用传入的参数映射（参数名 -> 参数值）
     * @return 绑定后的参数数组，可直接用于 {@link Method#invoke} 调用
     * @throws IllegalArgumentException 当必需参数缺失时抛出
     */
    private Object[] resolveMethodArguments(Method method, Map<String, Object> arguments) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            com.mcp.annotation.Param paramAnnotation = param.getAnnotation(com.mcp.annotation.Param.class);

            if (paramAnnotation != null) {
                String paramName = paramAnnotation.name();
                Object value = arguments.get(paramName);

                if (value != null) {
                    args[i] = convertValue(value, param.getType());
                } else if (paramAnnotation.required()) {
                    throw new IllegalArgumentException("Required parameter missing: " + paramName);
                }
            } else {
                // 尝试按名称匹配
                Object value = arguments.get(param.getName());
                if (value != null) {
                    args[i] = convertValue(value, param.getType());
                }
            }
        }

        return args;
    }

    /**
     * 将 MCP 调用传入的参数值转换为目标 Java 类型。
     * <p>
     * 支持的类型转换包括：{@code String}、{@code int/Integer}、{@code long/Long}、
     * {@code double/Double}、{@code boolean/Boolean}。
     * 如果值已经是目标类型则直接返回，否则通过字符串中间表示进行解析。
     * </p>
     *
     * @param value      原始参数值
     * @param targetType 目标 Java 类型
     * @return 转换后的参数值
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (targetType.isInstance(value)) {
            return value;
        }

        String strValue = String.valueOf(value);

        if (targetType == String.class) {
            return strValue;
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(strValue);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(strValue);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(strValue);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(strValue);
        }

        return value;
    }
}
