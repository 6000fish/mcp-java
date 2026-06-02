package com.mcp.annotation;

import java.lang.annotation.*;

/**
 * 标记一个类为 MCP 服务器。
 * <p>
 * 被此注解标记的类将成为 MCP 服务器的入口，框架会在启动时扫描该类及其方法，
 * 将带有 {@link McpTool}、{@link McpResource}、{@link McpPrompt} 注解的方法
 * 自动注册为 MCP 服务器的能力。
 * </p>
 * <p>
 * 服务器的名称和版本信息会在 MCP 初始化握手阶段通过 {@code initialize} 请求
 * 返回给客户端。
 * </p>
 *
 * <pre>
 * 示例：
 * &#64;McpServer(name = "my-mcp-server", version = "1.0.0")
 * public class MyMcpServer {
 *     &#64;McpTool(name = "hello", description = "打招呼")
 *     public String hello(&#64;Param(name = "name") String name) {
 *         return "Hello, " + name;
 *     }
 * }
 * </pre>
 *
 * @see McpTool
 * @see McpResource
 * @see McpPrompt
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpServer {

    /**
     * MCP 服务器的名称。
     * <p>
     * 该名称会在初始化握手时发送给客户端，用于标识服务器的身份。
     * </p>
     *
     * @return 服务器名称
     */
    String name();

    /**
     * MCP 服务器的版本号。
     * <p>
     * 遵循语义化版本号规范（如 {@code "1.0.0"}），在初始化握手时发送给客户端。
     * </p>
     *
     * @return 版本号字符串，默认为 {@code "1.0.0"}
     */
    String version() default "1.0.0";
}
