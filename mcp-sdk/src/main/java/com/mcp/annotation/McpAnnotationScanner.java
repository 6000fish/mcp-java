package com.mcp.annotation;

import com.mcp.protocol.PromptResult;
import com.mcp.protocol.ResourceContent;
import com.mcp.protocol.Tool;
import com.mcp.protocol.ToolCallResult;
import com.mcp.server.McpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP 注解扫描器。
 * <p>
 * 独立于 Spring 的注解处理器，扫描带 {@code @McpTool}、{@code @McpResource}、{@code @McpPrompt}
 * 注解的方法，自动注册到 {@link McpServer} 实例。
 * </p>
 * <p>
 * 典型用法：
 * <pre>
 * MyMcpServer myServer = new MyMcpServer();
 * McpServer server = DefaultMcpServer.builder().name("my-server").version("1.0.0").build();
 * McpAnnotationScanner.scan(server, myServer);
 * server.start(new StdioTransport());
 * </pre>
 * </p>
 */
public class McpAnnotationScanner {

    private static final Logger log = LoggerFactory.getLogger(McpAnnotationScanner.class);

    /** URI 模板参数占位符正则，匹配 {paramName} */
    private static final Pattern URI_PARAM_PATTERN = Pattern.compile("\\{([^}]+)}");

    private McpAnnotationScanner() {
        // 工具类，禁止实例化
    }

    /**
     * 扫描对象实例上的注解方法，注册到 MCP Server。
     *
     * @param server   目标 MCP Server
     * @param instance 带注解方法的对象实例
     */
    public static void scan(McpServer server, Object instance) {
        Class<?> clazz = instance.getClass();
        com.mcp.annotation.McpServer serverAnnotation = clazz.getAnnotation(com.mcp.annotation.McpServer.class);
        String serverName = serverAnnotation != null ? serverAnnotation.name() : clazz.getSimpleName();
        log.info("Scanning annotations for: {}", serverName);

        registerTools(server, instance, clazz);
        registerResources(server, instance, clazz);
        registerPrompts(server, instance, clazz);
    }

    /**
     * 扫描并注册所有带 {@code @McpTool} 注解的方法。
     */
    private static void registerTools(McpServer server, Object instance, Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            McpTool annotation = method.getAnnotation(McpTool.class);
            if (annotation == null) continue;

            String toolName = annotation.name();
            String description = annotation.description();
            method.setAccessible(true);

            Tool.InputSchema inputSchema = createInputSchema(method);

            server.tool(toolName, description, inputSchema, arguments -> {
                try {
                    Object[] args = resolveMethodArguments(method, arguments);
                    Object result = method.invoke(instance, args);

                    if (result instanceof ToolCallResult tcr) {
                        return tcr;
                    }
                    return ToolCallResult.success(String.valueOf(result));
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    return ToolCallResult.error("Tool execution failed: " + cause.getMessage());
                }
            });

            log.debug("Registered tool: {}", toolName);
        }
    }

    /**
     * 扫描并注册所有带 {@code @McpResource} 注解的方法。
     */
    private static void registerResources(McpServer server, Object instance, Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            McpResource annotation = method.getAnnotation(McpResource.class);
            if (annotation == null) continue;

            String uri = annotation.uri();
            String name = annotation.name().isEmpty() ? method.getName() : annotation.name();
            String description = annotation.description();
            String mimeType = annotation.mimeType();
            method.setAccessible(true);

            server.resource(uri, name, description, resourceUri -> {
                try {
                    Map<String, Object> uriParams = extractUriParams(uri, resourceUri);
                    Object[] args = resolveMethodArguments(method, uriParams);
                    Object result = method.invoke(instance, args);

                    if (result instanceof ResourceContent rc) {
                        return rc;
                    }
                    return ResourceContent.builder()
                            .uri(resourceUri)
                            .mimeType(mimeType)
                            .text(String.valueOf(result))
                            .build();
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    return ResourceContent.text(resourceUri, "Error: " + cause.getMessage());
                }
            });

            log.debug("Registered resource: {} -> {}", name, uri);
        }
    }

    /**
     * 扫描并注册所有带 {@code @McpPrompt} 注解的方法。
     */
    private static void registerPrompts(McpServer server, Object instance, Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            McpPrompt annotation = method.getAnnotation(McpPrompt.class);
            if (annotation == null) continue;

            String promptName = annotation.name();
            String description = annotation.description();
            method.setAccessible(true);

            server.prompt(promptName, description, arguments -> {
                try {
                    Map<String, Object> argsMap = new HashMap<>(arguments);
                    Object[] args = resolveMethodArguments(method, argsMap);
                    Object result = method.invoke(instance, args);

                    if (result instanceof PromptResult pr) {
                        return pr;
                    }
                    return PromptResult.builder()
                            .description(description)
                            .messages(java.util.List.of(
                                    PromptResult.Message.builder()
                                            .role("user")
                                            .content(PromptResult.Content.text(String.valueOf(result)))
                                            .build()
                            ))
                            .build();
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    return PromptResult.builder()
                            .description("Error: " + cause.getMessage())
                            .messages(java.util.List.of())
                            .build();
                }
            });

            log.debug("Registered prompt: {}", promptName);
        }
    }

    private static Tool.InputSchema createInputSchema(Method method) {
        Map<String, Tool.Property> properties = new HashMap<>();
        List<String> required = new ArrayList<>();

        for (Parameter parameter : method.getParameters()) {
            Param param = parameter.getAnnotation(Param.class);
            String name = param != null ? param.name() : parameter.getName();
            String description = param != null ? param.description() : "";

            properties.put(name, Tool.Property.builder()
                    .type(toJsonSchemaType(parameter.getType()))
                    .description(description)
                    .build());

            if (param == null || param.required()) {
                required.add(name);
            }
        }

        return Tool.InputSchema.builder()
                .type("object")
                .properties(properties)
                .required(required)
                .build();
    }

    private static String toJsonSchemaType(Class<?> type) {
        if (type == String.class || type == Character.class || type == char.class) {
            return "string";
        }
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class
                || type == short.class || type == Short.class || type == byte.class || type == Byte.class) {
            return "integer";
        }
        if (type == float.class || type == Float.class || type == double.class || type == Double.class) {
            return "number";
        }
        if (type == boolean.class || type == Boolean.class) {
            return "boolean";
        }
        if (type.isArray() || Iterable.class.isAssignableFrom(type)) {
            return "array";
        }
        return "object";
    }

    /**
     * 解析方法参数，将调用参数映射到 Java 方法参数。
     * <p>
     * 参数绑定优先级：
     * <ol>
     *   <li>通过 {@code @Param} 注解的 name 属性匹配</li>
     *   <li>通过方法参数名称匹配</li>
     * </ol>
     * </p>
     */
    private static Object[] resolveMethodArguments(Method method, Map<String, Object> arguments) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Param paramAnnotation = param.getAnnotation(Param.class);

            if (paramAnnotation != null) {
                String paramName = paramAnnotation.name();
                Object value = arguments.get(paramName);

                if (value != null) {
                    args[i] = convertValue(value, param.getType());
                } else if (paramAnnotation.required()) {
                    throw new IllegalArgumentException("Required parameter missing: " + paramName);
                }
            } else {
                Object value = arguments.get(param.getName());
                if (value != null) {
                    args[i] = convertValue(value, param.getType());
                }
            }
        }

        return args;
    }

    /**
     * 从 URI 模板中提取参数。
     * <p>
     * 将模板 URI（如 {@code file:///{path}}）与实际 URI（如 {@code file:///etc/hosts}）进行匹配，
     * 提取出占位符对应的值。
     * </p>
     *
     * @param templateUri URI 模板
     * @param actualUri   实际 URI
     * @return 参数映射
     */
    private static Map<String, Object> extractUriParams(String templateUri, String actualUri) {
        Map<String, Object> params = new HashMap<>();

        // 将模板转换为正则：{name} -> ([^/]+)
        String regex = templateUri;
        Matcher matcher = URI_PARAM_PATTERN.matcher(templateUri);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "(.+)");
        }
        matcher.appendTail(sb);
        regex = sb.toString();

        Pattern pattern = Pattern.compile(regex);
        Matcher match = pattern.matcher(actualUri);

        if (match.matches()) {
            Matcher paramMatcher = URI_PARAM_PATTERN.matcher(templateUri);
            int groupIndex = 1;
            while (paramMatcher.find()) {
                String paramName = paramMatcher.group(1);
                params.put(paramName, match.group(groupIndex));
                groupIndex++;
            }
        }

        return params;
    }

    /**
     * 参数类型转换。
     */
    private static Object convertValue(Object value, Class<?> targetType) {
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
