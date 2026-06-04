package com.mcp.server;

import com.mcp.protocol.*;
import com.mcp.transport.Transport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultMcpServer 单元测试
 */
class DefaultMcpServerTest {

    private DefaultMcpServer server;
    private MockTransport transport;

    @BeforeEach
    void setUp() throws Exception {
        server = DefaultMcpServer.builder()
                .name("test-server")
                .version("1.0.0")
                .build();

        server.tool("greet", "打招呼", params ->
                ToolCallResult.success("Hello, " + params.getOrDefault("name", "World")));
        server.resource("config://app", "app-config", "应用配置", uri ->
                ResourceContent.text(uri, "config-content"));
        server.prompt("review", "代码审查", params ->
                PromptResult.builder()
                        .description("Review " + params.getOrDefault("language", "Java"))
                        .messages(List.of(
                                PromptResult.Message.builder()
                                        .role("user")
                                        .content(PromptResult.Content.text("Please review"))
                                        .build()
                        ))
                        .build());

        transport = new MockTransport();
        server.start(transport);
    }

    @Test
    void testStartRegistersCallbacksAndStartsTransport() {
        assertTrue(transport.started);
        assertNotNull(transport.messageHandler);
        assertNotNull(transport.errorHandler);
        assertNotNull(transport.closeHandler);
    }

    @Test
    void testInitializeRequest() {
        JsonRpcMessage response = sendRequest(1, McpMethods.INITIALIZE, Map.of());

        assertTrue(response.isResponse());
        assertEquals(1, response.getId());
        assertNull(response.getError());
        assertInstanceOf(InitializeResult.class, response.getResult());

        InitializeResult result = (InitializeResult) response.getResult();
        assertEquals("2024-11-05", result.getProtocolVersion());
        assertEquals("test-server", result.getServerInfo().getName());
        assertEquals("1.0.0", result.getServerInfo().getVersion());
        assertNotNull(result.getCapabilities().getTools());
        assertNotNull(result.getCapabilities().getResources());
        assertNotNull(result.getCapabilities().getPrompts());
    }

    @Test
    void testToolsListRequest() {
        JsonRpcMessage response = sendRequest(2, McpMethods.TOOLS_LIST, null);

        assertNull(response.getError());
        Map<String, Object> result = castMap(response.getResult());
        List<Tool> tools = castList(result.get("tools"));

        assertEquals(1, tools.size());
        Tool tool = tools.get(0);
        assertEquals("greet", tool.getName());
        assertEquals("打招呼", tool.getDescription());
        assertNotNull(tool.getInputSchema());
        assertEquals("object", tool.getInputSchema().getType());
        assertTrue(tool.getInputSchema().getProperties().isEmpty());
        assertTrue(tool.getInputSchema().getRequired().isEmpty());
    }

    @Test
    void testToolsCallRequest() {
        JsonRpcMessage response = sendRequest(3, McpMethods.TOOLS_CALL, Map.of(
                "name", "greet",
                "arguments", Map.of("name", "MCP")
        ));

        assertNull(response.getError());
        assertInstanceOf(ToolCallResult.class, response.getResult());

        ToolCallResult result = (ToolCallResult) response.getResult();
        assertFalse(result.getIsError());
        assertEquals("Hello, MCP", result.getContent().get(0).getText());
    }

    @Test
    void testToolsCallUnknownToolReturnsError() {
        JsonRpcMessage response = sendRequest(4, McpMethods.TOOLS_CALL, Map.of(
                "name", "missing",
                "arguments", Map.of()
        ));

        assertNotNull(response.getError());
        assertEquals(-32601, response.getError().getCode());
        assertTrue(response.getError().getMessage().contains("missing"));
    }

    @Test
    void testResourcesListRequest() {
        JsonRpcMessage response = sendRequest(5, McpMethods.RESOURCES_LIST, null);

        assertNull(response.getError());
        Map<String, Object> result = castMap(response.getResult());
        List<Resource> resources = castList(result.get("resources"));

        assertEquals(1, resources.size());
        assertEquals("config://app", resources.get(0).getUri());
        assertEquals("app-config", resources.get(0).getName());
    }

    @Test
    void testResourcesReadRequest() {
        JsonRpcMessage response = sendRequest(6, McpMethods.RESOURCES_READ, Map.of("uri", "config://app"));

        assertNull(response.getError());
        Map<String, Object> result = castMap(response.getResult());
        List<ResourceContent> contents = castList(result.get("contents"));

        assertEquals(1, contents.size());
        assertEquals("config://app", contents.get(0).getUri());
        assertEquals("config-content", contents.get(0).getText());
    }

    @Test
    void testResourcesReadUnknownResourceReturnsError() {
        JsonRpcMessage response = sendRequest(7, McpMethods.RESOURCES_READ, Map.of("uri", "missing://resource"));

        assertNotNull(response.getError());
        assertEquals(-32601, response.getError().getCode());
        assertTrue(response.getError().getMessage().contains("missing://resource"));
    }

    @Test
    void testPromptsListRequest() {
        JsonRpcMessage response = sendRequest(8, McpMethods.PROMPTS_LIST, null);

        assertNull(response.getError());
        Map<String, Object> result = castMap(response.getResult());
        List<Prompt> prompts = castList(result.get("prompts"));

        assertEquals(1, prompts.size());
        assertEquals("review", prompts.get(0).getName());
        assertEquals("代码审查", prompts.get(0).getDescription());
    }

    @Test
    void testPromptsGetRequest() {
        JsonRpcMessage response = sendRequest(9, McpMethods.PROMPTS_GET, Map.of(
                "name", "review",
                "arguments", Map.of("language", "Python")
        ));

        assertNull(response.getError());
        assertInstanceOf(PromptResult.class, response.getResult());

        PromptResult result = (PromptResult) response.getResult();
        assertEquals("Review Python", result.getDescription());
        assertEquals("user", result.getMessages().get(0).getRole());
        assertEquals("Please review", result.getMessages().get(0).getContent().getText());
    }

    @Test
    void testPromptsGetUnknownPromptReturnsError() {
        JsonRpcMessage response = sendRequest(10, McpMethods.PROMPTS_GET, Map.of("name", "missing"));

        assertNotNull(response.getError());
        assertEquals(-32601, response.getError().getCode());
        assertTrue(response.getError().getMessage().contains("missing"));
    }

    @Test
    void testPingRequest() {
        JsonRpcMessage response = sendRequest(11, McpMethods.PING, null);

        assertNull(response.getError());
        assertInstanceOf(Map.class, response.getResult());
        assertTrue(castMap(response.getResult()).isEmpty());
    }

    @Test
    void testUnknownMethodReturnsMethodNotFound() {
        JsonRpcMessage response = sendRequest(12, "unknown/method", null);

        assertNotNull(response.getError());
        assertEquals(-32601, response.getError().getCode());
        assertTrue(response.getError().getMessage().contains("unknown/method"));
    }

    @Test
    void testNotificationDoesNotSendResponse() {
        transport.simulateIncoming(JsonRpcMessage.notification(McpMethods.INITIALIZED, null));

        assertTrue(transport.sentMessages.isEmpty());
    }

    @Test
    void testStopClosesTransport() throws Exception {
        server.stop();

        assertFalse(transport.connected);
    }

    @Test
    void testToolsCallIgnoresClientMeta() {
        JsonRpcMessage response = sendRequest(13, McpMethods.TOOLS_CALL, Map.of(
                "name", "greet",
                "arguments", Map.of("name", "MCP"),
                "_meta", Map.of("progressToken", "codex-test")
        ));

        assertNull(response.getError());
        assertInstanceOf(ToolCallResult.class, response.getResult());

        ToolCallResult result = (ToolCallResult) response.getResult();
        assertFalse(result.getIsError());
        assertEquals("Hello, MCP", result.getContent().get(0).getText());
    }

    @Test
    void testInitializeIgnoresClientExtensions() {
        JsonRpcMessage response = sendRequest(14, McpMethods.INITIALIZE, Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(
                        "roots", Map.of("listChanged", true, "extra", true),
                        "experimental", Map.of("feature", true)
                ),
                "clientInfo", Map.of("name", "codex", "version", "test", "extra", "ignored"),
                "_meta", Map.of("client", "compat-test")
        ));

        assertNull(response.getError());
        assertInstanceOf(InitializeResult.class, response.getResult());
    }

    @Test
    void testToolsCallAllowsMissingArguments() {
        JsonRpcMessage response = sendRequest(15, McpMethods.TOOLS_CALL, Map.of("name", "greet"));

        assertNull(response.getError());
        assertInstanceOf(ToolCallResult.class, response.getResult());

        ToolCallResult result = (ToolCallResult) response.getResult();
        assertFalse(result.getIsError());
        assertEquals("Hello, World", result.getContent().get(0).getText());
    }

    @Test
    void testResourcesReadIgnoresClientMeta() {
        JsonRpcMessage response = sendRequest(16, McpMethods.RESOURCES_READ, Map.of(
                "uri", "config://app",
                "_meta", Map.of("progressToken", "resource-test")
        ));

        assertNull(response.getError());
        Map<String, Object> result = castMap(response.getResult());
        List<ResourceContent> contents = castList(result.get("contents"));
        assertEquals("config-content", contents.get(0).getText());
    }

    @Test
    void testPromptsGetConvertsArgumentValuesAndIgnoresClientMeta() {
        JsonRpcMessage response = sendRequest(17, McpMethods.PROMPTS_GET, Map.of(
                "name", "review",
                "arguments", Map.of("language", 123),
                "_meta", Map.of("progressToken", "prompt-test")
        ));

        assertNull(response.getError());
        assertInstanceOf(PromptResult.class, response.getResult());

        PromptResult result = (PromptResult) response.getResult();
        assertEquals("Review 123", result.getDescription());
    }

    private JsonRpcMessage sendRequest(Object id, String method, Object params) {
        transport.sentMessages.clear();
        transport.simulateIncoming(JsonRpcMessage.request(id, method, params));
        assertEquals(1, transport.sentMessages.size());
        return transport.sentMessages.get(0);
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> castList(Object value) {
        return (List<T>) value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    static class MockTransport implements Transport {
        private Consumer<JsonRpcMessage> messageHandler;
        private Consumer<Throwable> errorHandler;
        private Runnable closeHandler;
        private boolean started;
        private boolean connected;
        private final List<JsonRpcMessage> sentMessages = new ArrayList<>();

        @Override
        public void start() {
            started = true;
            connected = true;
        }

        @Override
        public void send(JsonRpcMessage message) {
            sentMessages.add(message);
        }

        @Override
        public void onMessage(Consumer<JsonRpcMessage> handler) {
            this.messageHandler = handler;
        }

        @Override
        public void onError(Consumer<Throwable> handler) {
            this.errorHandler = handler;
        }

        @Override
        public void onClose(Runnable handler) {
            this.closeHandler = handler;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public void close() throws IOException {
            connected = false;
            if (closeHandler != null) {
                closeHandler.run();
            }
        }

        private void simulateIncoming(JsonRpcMessage message) {
            messageHandler.accept(message);
        }
    }
}
