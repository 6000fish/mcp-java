package com.mcp.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot MCP 示例应用启动类。
 * <p>
 * 演示如何在 Spring Boot 应用中集成 MCP Server。
 * 通过 {@code @SpringBootApplication} 启动自动配置机制，
 * {@link com.mcp.autoconfigure.McpAutoConfiguration} 会自动扫描
 * 容器中带 {@code @McpServer} 注解的 Bean 并注册其工具。
 * </p>
 * <p>
 * 配置项通过 {@code application.yml} 中的 {@code mcp.*} 前缀指定。
 * </p>
 */
@SpringBootApplication
public class SpringBootExampleApplication {

    /**
     * 程序入口，启动 Spring Boot 应用。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SpringBootExampleApplication.class, args);
    }
}
