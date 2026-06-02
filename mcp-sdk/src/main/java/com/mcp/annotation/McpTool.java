package com.mcp.annotation;

import java.lang.annotation.*;

/**
 * 标记一个方法为 MCP 工具（Tool）。
 * <p>
 * 被此注解标记的方法将被注册为 MCP 协议中的可调用工具，客户端可以通过
 * {@code tools/list} 发现工具，通过 {@code tools/call} 调用工具。
 * </p>
 * <p>
 * 方法的参数可使用 {@link Param} 注解来声明参数的名称、描述和是否必填。
 * </p>
 *
 * <pre>
 * 示例：
 * &#64;McpTool(name = "get_weather", description = "获取指定城市的天气信息")
 * public WeatherResult getWeather(&#64;Param(name = "city", description = "城市名称") String city) {
 *     // ...
 * }
 * </pre>
 *
 * @see Param
 * @see McpResource
 * @see McpPrompt
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpTool {

    /**
     * 工具名称，在同一 MCP 服务器中必须唯一。
     * <p>
     * 该名称用于客户端识别和调用工具，建议使用小写字母和下划线的命名风格。
     * </p>
     *
     * @return 工具名称
     */
    String name();

    /**
     * 工具的功能描述，用于向客户端说明该工具的用途。
     * <p>
     * 描述信息会通过 {@code tools/list} 返回给客户端，帮助 LLM 决定何时调用该工具。
     * </p>
     *
     * @return 工具描述，默认为空字符串
     */
    String description() default "";
}
