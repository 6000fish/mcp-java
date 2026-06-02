package com.mcp.annotation;

import java.lang.annotation.*;

/**
 * 标记一个方法为 MCP 工具
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpTool {

    /**
     * 工具名称
     */
    String name();

    /**
     * 工具描述
     */
    String description() default "";
}
