package com.mcp.annotation;

import java.lang.annotation.*;

/**
 * 标记一个类为 MCP Server
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpServer {

    /**
     * Server 名称
     */
    String name();

    /**
     * Server 版本
     */
    String version() default "1.0.0";
}
