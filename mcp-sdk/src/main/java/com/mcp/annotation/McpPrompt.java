package com.mcp.annotation;

import java.lang.annotation.*;

/**
 * 标记一个方法为 MCP Prompt
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpPrompt {

    /**
     * Prompt 名称
     */
    String name();

    /**
     * Prompt 描述
     */
    String description() default "";
}
