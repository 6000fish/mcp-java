package com.mcp.annotation;

import java.lang.annotation.*;

/**
 * 标记方法参数的元数据注解。
 * <p>
 * 用于为 {@link McpTool} 和 {@link McpPrompt} 注解的方法参数提供名称、描述和
 * 是否必填等信息。这些信息会被注册到 MCP 服务器的工具/提示定义中，帮助客户端
 * （尤其是 LLM）理解参数的含义并正确传参。
 * </p>
 *
 * <pre>
 * 示例：
 * &#64;McpTool(name = "search")
 * public Result search(&#64;Param(name = "query", description = "搜索关键词") String query,
 *                     &#64;Param(name = "limit", description = "返回数量", required = false) Integer limit) {
 *     // ...
 * }
 * </pre>
 *
 * @see McpTool
 * @see McpPrompt
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Param {

    /**
     * 参数名称，用于在 MCP 协议中标识该参数。
     * <p>
     * 该名称应与方法参数的实际用途一致，建议使用小写字母和下划线的命名风格。
     * </p>
     *
     * @return 参数名称
     */
    String name();

    /**
     * 参数的功能描述，用于向客户端说明该参数的含义、取值范围或使用说明。
     *
     * @return 参数描述，默认为空字符串
     */
    String description() default "";

    /**
     * 是否为必填参数。
     * <p>
     * 如果为 {@code true}，客户端调用时必须提供该参数值；
     * 如果为 {@code false}，该参数为可选参数，方法实现应能处理参数缺失的情况。
     * </p>
     *
     * @return {@code true} 表示必填，{@code false} 表示可选，默认为 {@code true}
     */
    boolean required() default true;
}
