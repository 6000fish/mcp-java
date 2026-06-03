package com.mcp.client;

import com.mcp.exception.McpException;
import com.mcp.protocol.*;
import com.mcp.transport.Transport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultMcpClient 单元测试
 *
 * 使用 MockTransport 模拟服务端响应，测试客户端所有方法：
 * 1. 连接生命周期（connect / disconnect）
 * 2. 初始化握手（initialize）
 * 3. 工具操作（listTools / callTool / ping）
 * 4. 资源操作（listResources / readResource）
 * 5. Prompt 操作（listPrompts / getPrompt）
 * 6. 错误处理（超时、错误响应、连接断开）
 * 7. 通知回调
 */
class DefaultMcpClientTest {

    private MockTransport transport;
    private DefaultMcpClient client;

    @BeforeEach
    void setUp() throws Exception {
        transport = new MockTransport();
        client = DefaultMcpClient.builder()
                .name("test-client")
                .version("1.0.0")
                .timeout(5)
                .build();
        client.connect(transport);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (client.isConnected()) {
            client.disconnect();
        }
    }

    // ==================== 1. 连接生命周期测试 ====================

    /**
     * 测试连接和断开的基本流程
     */
    @Test
    void testConnectAndDisconnect() throws Exception {
        assertTrue(client.isConnected());

        client.disconnect();
        assertFalse(client.isConnected());
    }

    /**
     * 测试断开后重复调用不会报错
     */
    @Test
    void testDisconnectIdempotent() throws Exception {
        client.disconnect();
        client.disconnect(); // 不应抛异常
        assertFalse(client.isConnected());
    }

    // ==================== 2. 初始化握手测试 ====================

    /**
     * 测试初始化握手流程
     */
    @Test
    void testInitialize() throws Exception {
        // 准备模拟响应
        InitializeResult expectedResult = InitializeResult.builder()
                .protocolVersion("2024-11-05")
                .serverInfo(Implementation.builder().name("test-server").version("1.0.0").build())
                .build();
        transport.setMockResult(expectedResult);

        // 执行初始化
        InitializeRequest request = InitializeRequest.builder()
                .protocolVersion("2024-11-05")
                .clientInfo(Implementation.builder().name("test-client").version("1.0.0").build())
                .build();
        InitializeResult result = client.initialize(request);

        assertNotNull(result);
        assertEquals("2024-11-05", result.getProtocolVersion());
        assertEquals("test-server", result.getServerInfo().getName());
    }

    // ==================== 3. 工具操作测试 ====================

    /**
     * 测试列出工具
     */
    @Test
    void testListTools() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("tools", List.of(
                Map.of("name", "greet", "description", "打招呼"),
                Map.of("name", "add", "description", "加法")
        ));
        transport.setMockResult(result);

        List<Tool> tools = client.listTools();

        assertNotNull(tools);
        assertEquals(2, tools.size());
        assertEquals("greet", tools.get(0).getName());
        assertEquals("add", tools.get(1).getName());
    }

    /**
     * 测试调用工具
     */
    @Test
    void testCallTool() throws Exception {
        ToolCallResult expectedResult = ToolCallResult.success("Hello, World");
        transport.setMockResult(expectedResult);

        ToolCallResult result = client.callTool("greet", Map.of("name", "World"));

        assertNotNull(result);
        assertFalse(result.getIsError());
        assertEquals("Hello, World", result.getContent().get(0).getText());
    }

    /**
     * 测试 ping 心跳
     */
    @Test
    void testPing() throws Exception {
        transport.setMockResult(new HashMap<>());

        boolean result = client.ping();

        assertTrue(result);
    }

    // ==================== 4. 资源操作测试 ====================

    /**
     * 测试列出资源
     */
    @Test
    void testListResources() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("resources", List.of(
                Map.of("uri", "file:///config", "name", "config", "description", "配置文件")
        ));
        transport.setMockResult(result);

        List<Resource> resources = client.listResources();

        assertNotNull(resources);
        assertEquals(1, resources.size());
        assertEquals("file:///config", resources.get(0).getUri());
    }

    /**
     * 测试读取资源
     */
    @Test
    void testReadResource() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("contents", List.of(
                Map.of("uri", "file:///config", "text", "{\"key\":\"value\"}", "mimeType", "application/json")
        ));
        transport.setMockResult(result);

        List<ResourceContent> contents = client.readResource("file:///config");

        assertNotNull(contents);
        assertEquals(1, contents.size());
        assertEquals("{\"key\":\"value\"}", contents.get(0).getText());
    }

    // ==================== 5. Prompt 操作测试 ====================

    /**
     * 测试列出 Prompt
     */
    @Test
    void testListPrompts() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("prompts", List.of(
                Map.of("name", "code_review", "description", "代码审查模板")
        ));
        transport.setMockResult(result);

        List<Prompt> prompts = client.listPrompts();

        assertNotNull(prompts);
        assertEquals(1, prompts.size());
        assertEquals("code_review", prompts.get(0).getName());
    }

    /**
     * 测试获取 Prompt
     */
    @Test
    void testGetPrompt() throws Exception {
        PromptResult expectedResult = PromptResult.builder()
                .description("代码审查")
                .messages(List.of(
                        PromptResult.Message.builder()
                                .role("user")
                                .content(PromptResult.Content.text("Please review this Java code"))
                                .build()
                ))
                .build();
        transport.setMockResult(expectedResult);

        PromptResult result = client.getPrompt("code_review", Map.of("language", "Java"));

        assertNotNull(result);
        assertEquals("代码审查", result.getDescription());
        assertEquals("Please review this Java code", result.getMessages().get(0).getContent().getText());
    }

    // ==================== 6. 错误处理测试 ====================

    /**
     * 测试服务端返回错误响应
     */
    @Test
    void testErrorResponse() {
        transport.setMockError(-32601, "Method not found");

        McpException ex = assertThrows(McpException.class, () -> client.listTools());
        assertEquals(-32601, ex.getCode());
        assertTrue(ex.getMessage().contains("Method not found"));
    }

    /**
     * 测试未连接时发送请求
     */
    @Test
    void testRequestWhenDisconnected() throws Exception {
        client.disconnect();

        assertThrows(IllegalStateException.class, () -> client.listTools());
    }

    // ==================== 7. 通知回调测试 ====================

    /**
     * 测试通知回调注册和分发
     */
    @Test
    void testNotificationCallback() {
        AtomicReference<JsonRpcMessage> received = new AtomicReference<>();
        client.onNotification(msg -> received.set(msg));

        // 模拟服务端发送通知
        JsonRpcMessage notification = JsonRpcMessage.notification("notifications/resources/updated", null);
        transport.simulateNotification(notification);

        assertNotNull(received.get());
        assertEquals("notifications/resources/updated", received.get().getMethod());
    }

    /**
     * 测试多个通知回调
     */
    @Test
    void testMultipleNotificationCallbacks() {
        AtomicReference<JsonRpcMessage> first = new AtomicReference<>();
        AtomicReference<JsonRpcMessage> second = new AtomicReference<>();

        client.onNotification(msg -> first.set(msg));
        client.onNotification(msg -> second.set(msg));

        JsonRpcMessage notification = JsonRpcMessage.notification("test", null);
        transport.simulateNotification(notification);

        assertNotNull(first.get());
        assertNotNull(second.get());
    }

    // ==================== Mock Transport ====================

    /**
     * 模拟传输层，用于测试客户端
     * <p>
     * 通过 {@link #setMockResult(Object)} 预设响应结果，
     * 客户端发送请求后会立即收到预设的响应。
     * </p>
     */
    static class MockTransport implements Transport {

        private Consumer<JsonRpcMessage> messageHandler;
        private boolean connected = true;
        private Object mockResult;
        private JsonRpcMessage.JsonRpcError mockError;

        void setMockResult(Object result) {
            this.mockResult = result;
            this.mockError = null;
        }

        void setMockError(int code, String message) {
            this.mockError = new JsonRpcMessage.JsonRpcError(code, message);
            this.mockResult = null;
        }

        void simulateNotification(JsonRpcMessage notification) {
            if (messageHandler != null) {
                messageHandler.accept(notification);
            }
        }

        @Override
        public void start() {
            // Mock: 已在构造时模拟连接
        }

        @Override
        public void send(JsonRpcMessage message) {
            if (!connected) throw new RuntimeException("Not connected");

            // 模拟异步响应：在新线程中发送，避免死锁
            if (message.isRequest()) {
                new Thread(() -> {
                    try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                    if (messageHandler != null && connected) {
                        JsonRpcMessage response;
                        if (mockError != null) {
                            response = JsonRpcMessage.errorResponse(
                                    message.getId(), mockError.getCode(), mockError.getMessage());
                        } else {
                            response = JsonRpcMessage.successResponse(message.getId(), mockResult);
                        }
                        messageHandler.accept(response);
                    }
                }).start();
            }
        }

        @Override
        public void onMessage(Consumer<JsonRpcMessage> handler) {
            this.messageHandler = handler;
        }

        @Override
        public void onError(Consumer<Throwable> handler) {
            // Mock: 不模拟错误
        }

        @Override
        public void onClose(Runnable handler) {
            // Mock: 不模拟关闭
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public void close() {
            connected = false;
        }
    }
}
