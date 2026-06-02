package com.mcp.example;

import com.mcp.annotation.McpServer;
import com.mcp.annotation.McpTool;
import com.mcp.annotation.Param;
import com.mcp.protocol.ToolCallResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 示例 MCP Server（Spring Boot 方式）。
 * <p>
 * 演示如何通过注解方式在 Spring Boot 中创建 MCP Server。
 * 本类使用 {@code @Component} 注解注册为 Spring Bean，
 * 使用 {@code @McpServer} 注解标记为 MCP Server 实现。
 * </p>
 * <p>
 * 提供以下 MCP 工具：
 * <ul>
 *   <li><b>hello</b> - 向指定人打招呼</li>
 *   <li><b>time</b> - 获取当前时间，支持自定义格式</li>
 *   <li><b>add</b> - 两数相加</li>
 *   <li><b>multiply</b> - 两数相乘</li>
 * </ul>
 * </p>
 * <p>
 * 通过 {@link com.mcp.autoconfigure.McpAutoConfiguration} 自动注册，无需手动启动。
 * </p>
 */
@Component
@McpServer(name = "spring-boot-example", version = "1.0.0")
public class ExampleMcpServer {

    /**
     * 向指定人打招呼。
     * <p>返回包含问候语和欢迎信息的字符串。方法返回值会自动包装为 {@link ToolCallResult}。</p>
     *
     * @param name 要问候的人的名称
     * @return 问候语字符串
     */
    @McpTool(name = "hello", description = "Say hello to someone")
    public String hello(
            @Param(name = "name", description = "Name to greet") String name
    ) {
        return "Hello, " + name + "! Welcome to Spring Boot MCP Server!";
    }

    /**
     * 获取当前时间。
     * <p>
     * 按指定格式返回当前日期时间。若未提供格式参数，默认使用 {@code "yyyy-MM-dd HH:mm:ss"}。
     * </p>
     *
     * @param format 时间格式模式，可选（如 {@code "yyyy/MM/dd HH:mm:ss"}）
     * @return 格式化后的当前时间字符串
     */
    @McpTool(name = "time", description = "Get current time")
    public String time(
            @Param(name = "format", description = "Time format", required = false) String format
    ) {
        String pattern = format != null ? format : "yyyy-MM-dd HH:mm:ss";
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 两数相加。
     *
     * @param a 第一个加数
     * @param b 第二个加数
     * @return 包含求和结果的 {@link ToolCallResult}
     */
    @McpTool(name = "add", description = "Add two numbers")
    public ToolCallResult add(
            @Param(name = "a", description = "First number") double a,
            @Param(name = "b", description = "Second number") double b
    ) {
        return ToolCallResult.success(String.valueOf(a + b));
    }

    /**
     * 两数相乘。
     *
     * @param a 第一个乘数
     * @param b 第二个乘数
     * @return 包含乘积结果的 {@link ToolCallResult}
     */
    @McpTool(name = "multiply", description = "Multiply two numbers")
    public ToolCallResult multiply(
            @Param(name = "a", description = "First number") double a,
            @Param(name = "b", description = "Second number") double b
    ) {
        return ToolCallResult.success(String.valueOf(a * b));
    }
}
