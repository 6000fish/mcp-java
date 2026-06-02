package com.mcp.autoconfigure;

import com.mcp.annotation.McpTool;
import com.mcp.protocol.ToolCallResult;
import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import com.mcp.transport.StdioTransport;
import com.mcp.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP 自动配置
 */
@AutoConfiguration
@ConditionalOnClass(McpServer.class)
@EnableConfigurationProperties(McpProperties.class)
public class McpAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(McpAutoConfiguration.class);

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

    @Bean
    @ConditionalOnMissingBean
    public Transport mcpTransport(McpProperties properties) {
        return switch (properties.getTransport()) {
            case STDIO -> new StdioTransport();
            case SSE -> {
                // TODO: 实现 SSE 传输
                log.warn("SSE transport not yet implemented, falling back to STDIO");
                yield new StdioTransport();
            }
        };
    }

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

    private void registerAnnotatedResources(McpServer server, ApplicationContext context) {
        // TODO: 实现资源注册
    }

    private void registerAnnotatedPrompts(McpServer server, ApplicationContext context) {
        // TODO: 实现 Prompt 注册
    }

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
