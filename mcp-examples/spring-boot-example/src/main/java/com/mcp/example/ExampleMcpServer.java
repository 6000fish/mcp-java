package com.mcp.example;

import com.mcp.annotation.McpServer;
import com.mcp.annotation.McpTool;
import com.mcp.annotation.Param;
import com.mcp.protocol.ToolCallResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 示例 MCP Server（Spring Boot 方式）
 */
@Component
@McpServer(name = "spring-boot-example", version = "1.0.0")
public class ExampleMcpServer {

    @McpTool(name = "hello", description = "Say hello to someone")
    public String hello(
            @Param(name = "name", description = "Name to greet") String name
    ) {
        return "Hello, " + name + "! Welcome to Spring Boot MCP Server!";
    }

    @McpTool(name = "time", description = "Get current time")
    public String time(
            @Param(name = "format", description = "Time format", required = false) String format
    ) {
        String pattern = format != null ? format : "yyyy-MM-dd HH:mm:ss";
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
    }

    @McpTool(name = "add", description = "Add two numbers")
    public ToolCallResult add(
            @Param(name = "a", description = "First number") double a,
            @Param(name = "b", description = "Second number") double b
    ) {
        return ToolCallResult.success(String.valueOf(a + b));
    }

    @McpTool(name = "multiply", description = "Multiply two numbers")
    public ToolCallResult multiply(
            @Param(name = "a", description = "First number") double a,
            @Param(name = "b", description = "Second number") double b
    ) {
        return ToolCallResult.success(String.valueOf(a * b));
    }
}
