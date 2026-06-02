package com.mcp.example;

import com.mcp.protocol.ToolCallResult;
import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import com.mcp.transport.StdioTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * MCP 快速入门示例。
 * <p>
 * 演示如何使用 MCP Java SDK 创建一个简单的 MCP Server，包含：
 * <ul>
 *   <li>注册多个示例工具（greet、current_time、calculate、text_transform）</li>
 *   <li>注册一个示例资源（server://info）</li>
 *   <li>通过 STDIO 传输层启动服务</li>
 * </ul>
 * </p>
 * <p>
 * 本示例适合初学者快速了解 MCP Server 的基本用法和工具注册方式。
 * </p>
 */
public class QuickStartServer {

    private static final Logger log = LoggerFactory.getLogger(QuickStartServer.class);

    /**
     * 程序入口。
     * <p>
     * 创建 MCP Server，注册示例工具和资源，启动 STDIO 传输层。
     * 主线程通过 {@code join()} 保持阻塞以维持服务运行。
     * </p>
     *
     * @param args 命令行参数（未使用）
     * @throws Exception 启动过程中可能抛出的异常
     */
    public static void main(String[] args) throws Exception {
        log.info("Starting MCP Quick Start Server...");

        // 创建 MCP Server
        McpServer server = DefaultMcpServer.builder()
                .name("quick-start-server")
                .version("1.0.0")
                .build();

        // 注册工具：问候 - 向指定名称的人打招呼，若未提供 name 参数则默认问候 "World"
        server.tool("greet", "Greet someone by name", arguments -> {
            String name = (String) arguments.getOrDefault("name", "World");
            return ToolCallResult.success("Hello, " + name + "!");
        });

        // 注册工具：获取当前时间 - 返回格式化的当前日期时间，支持自定义格式
        server.tool("current_time", "Get current date and time", arguments -> {
            String format = (String) arguments.getOrDefault("format", "yyyy-MM-dd HH:mm:ss");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            String time = LocalDateTime.now().format(formatter);
            return ToolCallResult.success(time);
        });

        // 注册工具：计算 - 支持加减乘除四则运算，包含除零检查
        server.tool("calculate", "Perform basic math operations", arguments -> {
            try {
                double a = Double.parseDouble(String.valueOf(arguments.get("a")));
                double b = Double.parseDouble(String.valueOf(arguments.get("b")));
                String op = (String) arguments.get("operation");

                double result = switch (op) {
                    case "add" -> a + b;
                    case "subtract" -> a - b;
                    case "multiply" -> a * b;
                    case "divide" -> {
                        if (b == 0) {
                            yield Double.NaN;
                        }
                        yield a / b;
                    }
                    default -> Double.NaN;
                };

                if (Double.isNaN(result)) {
                    return ToolCallResult.error("Invalid operation or division by zero");
                }
                return ToolCallResult.success(String.valueOf(result));
            } catch (Exception e) {
                return ToolCallResult.error("Invalid input: " + e.getMessage());
            }
        });

        // 注册工具：文本处理 - 支持大小写转换、反转、长度计算等文本操作
        server.tool("text_transform", "Transform text (uppercase, lowercase, reverse)", arguments -> {
            String text = (String) arguments.get("text");
            String transform = (String) arguments.getOrDefault("transform", "uppercase");

            if (text == null) {
                return ToolCallResult.error("Text is required");
            }

            String result = switch (transform) {
                case "uppercase" -> text.toUpperCase();
                case "lowercase" -> text.toLowerCase();
                case "reverse" -> new StringBuilder(text).reverse().toString();
                case "length" -> String.valueOf(text.length());
                default -> text;
            };

            return ToolCallResult.success(result);
        });

        // 注册资源：服务器信息 - 返回服务端名称、版本、启动时间、Java 版本和操作系统信息
        server.resource("server://info", "Server Info", "Get server information", uri -> {
            Map<String, Object> info = Map.of(
                    "name", "quick-start-server",
                    "version", "1.0.0",
                    "startTime", LocalDateTime.now().toString(),
                    "javaVersion", System.getProperty("java.version"),
                    "os", System.getProperty("os.name")
            );
            return com.mcp.protocol.ResourceContent.json(uri, info);
        });

        // 启动服务
        server.start(new StdioTransport());

        log.info("MCP Quick Start Server started!");
        log.info("Available tools: greet, current_time, calculate, text_transform");
        log.info("Available resources: server://info");

        // 保持运行
        Thread.currentThread().join();
    }
}
