package com.mcp.annotation;

import java.lang.annotation.*;

/**
 * 标记一个方法为 MCP 资源
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpResource {

    /**
     * 资源 URI 模板
     */
    String uri();

    /**
     * 资源名称
     */
    String name() default "";

    /**
     * 资源描述
     */
    String description() default "";

    /**
     * MIME 类型
     */
    String mimeType() default "text/plain";
}
