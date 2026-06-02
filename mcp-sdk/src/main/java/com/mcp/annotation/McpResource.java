package com.mcp.annotation;

import java.lang.annotation.*;

/**
 * 标记一个方法为 MCP 资源（Resource）。
 * <p>
 * 被此注解标记的方法将被注册为 MCP 协议中的可读取资源，客户端可以通过
 * {@code resources/list} 发现资源，通过 {@code resources/read} 读取资源内容。
 * </p>
 * <p>
 * 资源通过 URI 模板进行标识，支持路径参数占位符（如 {@code file:///{path}}）。
 * 方法返回值将作为资源的内容提供给客户端。
 * </p>
 *
 * <pre>
 * 示例：
 * &#64;McpResource(uri = "file:///{path}", name = "readFile", description = "读取文件内容", mimeType = "text/plain")
 * public String readFile(&#64;Param(name = "path", description = "文件路径") String path) {
 *     // ...
 * }
 * </pre>
 *
 * @see McpTool
 * @see McpPrompt
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpResource {

    /**
     * 资源的 URI 模板，用于唯一标识该资源。
     * <p>
     * 支持使用 {@code {paramName}} 形式的路径参数占位符，运行时会被替换为实际参数值。
     * 例如：{@code file:///{path}}、{@code db://users/{userId}/profile}。
     * </p>
     *
     * @return 资源 URI 模板字符串
     */
    String uri();

    /**
     * 资源的显示名称。
     * <p>
     * 如果未指定，默认使用方法名作为资源名称。
     * </p>
     *
     * @return 资源名称，默认为空字符串
     */
    String name() default "";

    /**
     * 资源的功能描述，用于向客户端说明该资源的内容和用途。
     *
     * @return 资源描述，默认为空字符串
     */
    String description() default "";

    /**
     * 资源内容的 MIME 类型。
     * <p>
     * 用于告知客户端资源内容的格式，以便正确解析和展示。
     * 常见值如 {@code text/plain}、{@code application/json}、{@code text/html} 等。
     * </p>
     *
     * @return MIME 类型字符串，默认为 {@code "text/plain"}
     */
    String mimeType() default "text/plain";
}
