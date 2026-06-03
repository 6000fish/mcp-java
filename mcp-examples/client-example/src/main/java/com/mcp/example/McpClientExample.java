package com.mcp.example;

import com.mcp.client.DefaultMcpClient;
import com.mcp.client.McpClient;
import com.mcp.protocol.*;
import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import com.mcp.transport.StdioTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;

/**
 * MCP 客户端使用示例
 * <p>
 * 演示如何使用 MCP Client SDK 连接服务器、调用工具、读取资源和获取 Prompt。
 * 本示例在同一进程中启动服务端和客户端，通过管道流（PipedStream）进行通信。
 * </p>
 */
public class McpClientExample {

    private static final Logger log = LoggerFactory.getLogger(McpClientExample.class);

    public static void main(String[] args) throws Exception {
        // ==================== 1. 创建服务端 ====================
        McpServer server = DefaultMcpServer.builder()
                .name("demo-server")
                .version("1.0.0")
                .build();

        // 注册工具
        server.tool("greet", "打招呼工具", params -> {
            String name = (String) params.getOrDefault("name", "World");
            return ToolCallResult.success("Hello, " + name + "!");
        });

        server.tool("add", "加法工具", params -> {
            int a = Integer.parseInt(String.valueOf(params.get("a")));
            int b = Integer.parseInt(String.valueOf(params.get("b")));
            return ToolCallResult.success(String.valueOf(a + b));
        });

        // 注册资源
        server.resource("config://app", "app-config", "应用配置", uri ->
                ResourceContent.json(uri, Map.of("name", "demo", "version", "1.0.0")));

        // 注册 Prompt
        server.prompt("code_review", "代码审查模板", params -> {
            String language = params.getOrDefault("language", "Java");
            return PromptResult.builder()
                    .description("代码审查")
                    .messages(java.util.List.of(
                            PromptResult.Message.builder()
                                    .role("user")
                                    .content(PromptResult.Content.text("Please review this " + language + " code"))
                                    .build()
                    ))
                    .build();
        });

        // ==================== 2. 通过管道连接客户端和服务端 ====================
        // 客户端输出 -> 服务端输入
        PipedOutputStream clientOutput = new PipedOutputStream();
        PipedInputStream serverInput = new PipedInputStream(clientOutput);

        // 服务端输出 -> 客户端输入
        PipedOutputStream serverOutput = new PipedOutputStream();
        PipedInputStream clientInput = new PipedInputStream(serverOutput);

        // 启动服务端（在后台线程）
        StdioTransport serverTransport = new StdioTransport(serverInput, serverOutput);
        Thread serverThread = new Thread(() -> {
            try {
                server.start(serverTransport);
            } catch (Exception e) {
                log.error("Server error", e);
            }
        }, "mcp-server");
        serverThread.setDaemon(true);
        serverThread.start();

        // 等待服务端启动
        Thread.sleep(200);

        // ==================== 3. 创建并使用客户端 ====================
        McpClient client = DefaultMcpClient.builder()
                .name("demo-client")
                .version("1.0.0")
                .timeout(10)
                .build();

        StdioTransport clientTransport = new StdioTransport(clientInput, clientOutput);
        client.connect(clientTransport);

        // 初始化握手
        log.info("=== 初始化握手 ===");
        InitializeResult initResult = client.initialize(InitializeRequest.builder()
                .protocolVersion("2024-11-05")
                .clientInfo(Implementation.builder().name("demo-client").version("1.0.0").build())
                .build());
        log.info("服务器: {} v{}", initResult.getServerInfo().getName(), initResult.getServerInfo().getVersion());

        // 列出工具
        log.info("\n=== 列出工具 ===");
        client.listTools().forEach(tool ->
                log.info("  工具: {} - {}", tool.getName(), tool.getDescription()));

        // 调用工具
        log.info("\n=== 调用工具 ===");
        ToolCallResult greetResult = client.callTool("greet", Map.of("name", "MCP"));
        log.info("  greet 结果: {}", greetResult.getContent().get(0).getText());

        ToolCallResult addResult = client.callTool("add", Map.of("a", 10, "b", 20));
        log.info("  add 结果: {}", addResult.getContent().get(0).getText());

        // 列出资源
        log.info("\n=== 列出资源 ===");
        client.listResources().forEach(res ->
                log.info("  资源: {} ({})", res.getName(), res.getUri()));

        // 读取资源
        log.info("\n=== 读取资源 ===");
        client.readResource("config://app").forEach(content ->
                log.info("  内容: {}", content.getText()));

        // 列出 Prompt
        log.info("\n=== 列出 Prompt ===");
        client.listPrompts().forEach(prompt ->
                log.info("  Prompt: {} - {}", prompt.getName(), prompt.getDescription()));

        // 获取 Prompt
        log.info("\n=== 获取 Prompt ===");
        PromptResult promptResult = client.getPrompt("code_review", Map.of("language", "Python"));
        log.info("  描述: {}", promptResult.getDescription());
        log.info("  消息: {}", promptResult.getMessages().get(0).getContent().getText());

        // 心跳检测
        log.info("\n=== 心跳检测 ===");
        boolean alive = client.ping();
        log.info("  连接状态: {}", alive ? "正常" : "异常");

        // 断开连接
        client.disconnect();
        log.info("\n已断开连接");
    }
}
