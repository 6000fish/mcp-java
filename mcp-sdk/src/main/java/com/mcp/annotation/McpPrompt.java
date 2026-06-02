package com.mcp.annotation;

import java.lang.annotation.*;

/**
 * 标记一个方法为 MCP Prompt（提示模板）。
 * <p>
 * 被此注解标记的方法将被注册为 MCP 协议中的提示模板，客户端可以通过
 * {@code prompts/list} 发现提示，通过 {@code prompts/get} 获取提示内容。
 * </p>
 * <p>
 * 方法的参数可使用 {@link Param} 注解来声明参数的名称、描述和是否必填。
 * 方法返回值应包含提示消息内容。
 * </p>
 *
 * @see McpTool
 * @see McpResource
 * @see Param
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpPrompt {

    /**
     * 提示模板的名称，在同一 MCP 服务器中必须唯一。
     * <p>
     * 该名称用于客户端识别和获取提示，建议使用小写字母和下划线的命名风格。
     * </p>
     *
     * @return 提示模板名称
     */
    String name();

    /**
     * 提示模板的功能描述，用于向客户端说明该提示的用途和适用场景。
     *
     * @return 提示模板描述，默认为空字符串
     */
    String description() default "";
}
