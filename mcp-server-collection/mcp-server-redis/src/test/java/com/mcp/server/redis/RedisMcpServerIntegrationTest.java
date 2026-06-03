package com.mcp.server.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.annotation.McpAnnotationScanner;
import com.mcp.protocol.JsonRpcMessage;
import com.mcp.protocol.McpMethods;
import com.mcp.protocol.ToolCallResult;
import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import com.mcp.transport.Transport;
import org.junit.jupiter.api.*;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis 真实环境集成测试
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisMcpServerIntegrationTest {

    private final Properties config = new Properties();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private RedisMcpServer redisServer;
    private MockTransport transport;
    private String host;
    private int port;
    private String password;

    /**
     * 加载本地 Redis 与可选大模型配置，并初始化测试数据和 MCP Server 实例。
     */
    @BeforeAll
    void loadConfig() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("integration-test.properties")) {
            Assumptions.assumeTrue(input != null, "缺少 integration-test.properties，跳过 Redis 集成测试");
            config.load(input);
        }

        host = config.getProperty("redis.host", "localhost");
        port = Integer.parseInt(config.getProperty("redis.port", "6379"));
        password = config.getProperty("redis.password", "");

        prepareRedis();
        redisServer = new RedisMcpServer(host, port, password);
    }

    /**
     * 每个测试用例前创建新的 MCP Server，并通过注解扫描注册 Redis 工具。
     */
    @BeforeEach
    void setUp() throws Exception {
        McpServer server = DefaultMcpServer.builder()
                .name("redis-it-server")
                .version("1.0.0")
                .build();
        McpAnnotationScanner.scan(server, redisServer);

        transport = new MockTransport();
        server.start(transport);
    }

    /**
     * 测试结束后关闭 Redis 连接池，并清理集成测试写入的测试 key。
     */
    @AfterAll
    void tearDown() {
        if (redisServer != null) {
            redisServer.close();
        }
        cleanRedis();
    }

    /**
     * 验证模型发起字符串读写类工具调用时，MCP 能通过真实 Redis 写入并读取数据。
     */
    @Test
    void testModelCanReadAndWriteStringThroughMcp() {
        ToolCallResult setResult = callTool("set", Map.of(
                "key", "mcp:it:user:1",
                "value", "Alice"
        ));
        assertFalse(setResult.getIsError());
        assertEquals("OK", setResult.getContent().get(0).getText());

        ToolCallResult getResult = callTool("get", Map.of("key", "mcp:it:user:1"));
        assertFalse(getResult.getIsError());
        assertEquals("Alice", getResult.getContent().get(0).getText());
    }

    /**
     * 验证模型发起 hash 读取类工具调用时，MCP 能通过真实 Redis 返回 hash 字段和完整内容。
     */
    @Test
    void testModelCanReadHashThroughMcp() {
        ToolCallResult hgetResult = callTool("hget", Map.of(
                "key", "mcp:it:user:profile",
                "field", "name"
        ));
        assertFalse(hgetResult.getIsError());
        assertEquals("Bob", hgetResult.getContent().get(0).getText());

        ToolCallResult hgetAllResult = callTool("hgetall", Map.of("key", "mcp:it:user:profile"));
        assertFalse(hgetAllResult.getIsError());
        assertTrue(hgetAllResult.getContent().get(0).getText().contains("Bob"));
        assertTrue(hgetAllResult.getContent().get(0).getText().contains("role"));
    }

    /**
     * 验证模型发起 key 探查类工具调用时，MCP 能通过真实 Redis 返回 key、类型和 TTL。
     */
    @Test
    void testModelCanInspectKeysAndTtlThroughMcp() {
        ToolCallResult keysResult = callTool("keys", Map.of("pattern", "mcp:it:*"));
        assertFalse(keysResult.getIsError());
        assertTrue(keysResult.getContent().get(0).getText().contains("mcp:it:user:profile"));

        ToolCallResult typeResult = callTool("type", Map.of("key", "mcp:it:user:profile"));
        assertFalse(typeResult.getIsError());
        assertEquals("hash", typeResult.getContent().get(0).getText());

        ToolCallResult ttlResult = callTool("ttl", Map.of("key", "mcp:it:session"));
        assertFalse(ttlResult.getIsError());
        assertTrue(ttlResult.getContent().get(0).getText().contains("seconds"));
    }

    /**
     * 验证 Redis MCP 工具会拒绝删除、宽泛 key 查询和全量读取，同时保留窄范围诊断能力。
     */
    @Test
    void testRedisSafetyPolicyRejectsUnsafeOperations() {
        ToolCallResult broadKeys = callTool("keys", Map.of("pattern", "*"));
        assertTrue(broadKeys.getIsError());
        assertTrue(broadKeys.getContent().get(0).getText().contains("broad key patterns"));

        ToolCallResult deleteResult = callTool("del", Map.of("keys", "mcp:it:user:1"));
        assertTrue(deleteResult.getIsError());
        assertTrue(deleteResult.getContent().get(0).getText().contains("disabled"));

        ToolCallResult getAfterDelete = callTool("get", Map.of("key", "mcp:it:user:1"));
        assertFalse(getAfterDelete.getIsError());
        assertEquals("Alice", getAfterDelete.getContent().get(0).getText());

        ToolCallResult allInfo = callTool("info", Map.of());
        assertTrue(allInfo.getIsError());
        assertTrue(allInfo.getContent().get(0).getText().contains("INFO section is required"));

        ToolCallResult serverInfo = callTool("info", Map.of("section", "server"));
        assertFalse(serverInfo.getIsError());
        assertTrue(serverInfo.getContent().get(0).getText().contains("redis_version"));

        ToolCallResult fullList = callTool("lrange", Map.of(
                "key", "mcp:it:list",
                "start", 0,
                "stop", -1
        ));
        assertTrue(fullList.getIsError());
        assertTrue(fullList.getContent().get(0).getText().contains("full-list reads"));
    }

    /**
     * 验证自然语言问题经大模型选择工具后，MCP 能执行模型返回的 Redis 工具调用。
     */
    @Test
    void testModelCanChooseRedisToolFromConversation() throws Exception {
        Assumptions.assumeTrue(isModelConfigured(), "缺少 LLM 配置，跳过大模型驱动的 Redis 集成测试");

        Map<String, Object> decision = askModelForToolCall("用户想查看 Redis 里 mcp:it:user:profile 这个 hash 的 name 字段。", """
                可用 MCP 工具：
                - hget: 参数 {"key":"...","field":"..."}，用于读取 Redis hash 字段。
                请只返回 JSON：{"tool":"hget","arguments":{"key":"...","field":"..."}}
                """);

        assertEquals("hget", decision.get("tool"));
        ToolCallResult result = callTool((String) decision.get("tool"), castArguments(decision.get("arguments")));

        assertFalse(result.getIsError());
        assertEquals("Bob", result.getContent().get(0).getText());
    }

    /**
     * 写入 Redis 集成测试所需的初始 key，保证测试数据可重复。
     */
    private void prepareRedis() {
        try (Jedis jedis = createJedis()) {
            jedis.del("mcp:it:user:1", "mcp:it:user:profile", "mcp:it:session", "mcp:it:list");
            jedis.set("mcp:it:user:1", "Alice");
            jedis.rpush("mcp:it:list", "one", "two", "three");
            jedis.hset("mcp:it:user:profile", Map.of(
                    "name", "Bob",
                    "role", "admin"
            ));
            jedis.setex("mcp:it:session", 60, "active");
        }
    }

    /**
     * 删除 Redis 集成测试写入的 key，避免污染用户本地 Redis。
     */
    private void cleanRedis() {
        if (host == null) {
            return;
        }
        try (Jedis jedis = createJedis()) {
            jedis.del("mcp:it:user:1", "mcp:it:user:profile", "mcp:it:session", "mcp:it:list");
        }
    }

    /**
     * 根据配置创建 Jedis 连接，支持无密码和有密码两种本地 Redis。
     */
    private Jedis createJedis() {
        Jedis jedis = new Jedis(host, port);
        if (password != null && !password.isEmpty()) {
            jedis.auth(password);
        }
        return jedis;
    }

    /**
     * 判断是否配置了可用于真实调用的大模型接口。
     */
    private boolean isModelConfigured() {
        return hasText("llm.base-url") && hasText("llm.api-key") && hasText("llm.model");
    }

    /**
     * 判断指定配置项是否存在非空文本。
     */
    private boolean hasText(String key) {
        String value = config.getProperty(key);
        return value != null && !value.isBlank();
    }

    /**
     * 调用 OpenAI 兼容的大模型接口，让模型基于自然语言和工具说明生成 MCP 工具调用 JSON。
     */
    private Map<String, Object> askModelForToolCall(String userMessage, String systemPrompt) throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(buildModelRequest(userMessage, systemPrompt), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());

        String content = modelResponseText(response.body());
        return objectMapper.readValue(extractJson(content), new TypeReference<>() {});
    }

    /**
     * 根据 llm.provider 构造 OpenAI 或 Anthropic 兼容的大模型请求。
     */
    private HttpRequest buildModelRequest(String userMessage, String systemPrompt) throws Exception {
        String provider = config.getProperty("llm.provider", "openai").trim().toLowerCase();
        if ("anthropic".equals(provider)) {
            return buildAnthropicRequest(userMessage, systemPrompt);
        }
        return buildOpenAiRequest(userMessage, systemPrompt);
    }

    /**
     * 构造 OpenAI 兼容 chat completions 请求。
     */
    private HttpRequest buildOpenAiRequest(String userMessage, String systemPrompt) throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", config.getProperty("llm.model", ""),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "temperature", 0
        ));

        return HttpRequest.newBuilder()
                .uri(URI.create(openAiChatCompletionsUrl()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getProperty("llm.api-key"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    /**
     * 构造 Anthropic 兼容 messages 请求，适配 Claude Code 中转站配置。
     */
    private HttpRequest buildAnthropicRequest(String userMessage, String systemPrompt) throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", config.getProperty("llm.model", ""),
                "max_tokens", 512,
                "temperature", 0,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", systemPrompt + "\n\n" + userMessage
                ))
        ));

        return HttpRequest.newBuilder()
                .uri(URI.create(anthropicMessagesUrl()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getProperty("llm.api-key"))
                .header("anthropic-version", config.getProperty("llm.anthropic-version", "2023-06-01"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    /**
     * 从 OpenAI 或 Anthropic 兼容响应中提取模型输出文本。
     */
    private String modelResponseText(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String provider = config.getProperty("llm.provider", "openai").trim().toLowerCase();
        if ("anthropic".equals(provider)) {
            for (JsonNode item : root.path("content")) {
                if (item.has("text")) {
                    return item.path("text").asText();
                }
            }
            return "";
        }
        return root.path("choices").get(0).path("message").path("content").asText();
    }

    /**
     * 基于 llm.base-url 拼接 OpenAI 兼容的 chat completions 地址。
     */
    private String openAiChatCompletionsUrl() {
        String baseUrl = config.getProperty("llm.base-url").trim();
        if (baseUrl.endsWith("/chat/completions")) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/")) {
            return baseUrl + "chat/completions";
        }
        return baseUrl + "/chat/completions";
    }

    /**
     * 基于 llm.base-url 拼接 Anthropic 兼容的 messages 地址。
     */
    private String anthropicMessagesUrl() {
        String baseUrl = config.getProperty("llm.base-url").trim();
        if (baseUrl.endsWith("/messages")) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/")) {
            return baseUrl + "v1/messages";
        }
        return baseUrl + "/v1/messages";
    }

    /**
     * 从模型响应文本中提取首尾大括号之间的 JSON 内容。
     */
    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        assertTrue(start >= 0 && end > start, "模型响应中未找到 JSON: " + content);
        return content.substring(start, end + 1);
    }

    /**
     * 将模型返回的 arguments 字段转换为工具调用参数映射。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> castArguments(Object arguments) {
        return (Map<String, Object>) arguments;
    }

    /**
     * 模拟客户端通过 MCP tools/call 请求调用指定 Redis 工具。
     */
    private ToolCallResult callTool(String name, Map<String, Object> arguments) {
        transport.sentMessages.clear();
        transport.simulateIncoming(JsonRpcMessage.request(1, McpMethods.TOOLS_CALL, Map.of(
                "name", name,
                "arguments", arguments
        )));

        assertEquals(1, transport.sentMessages.size());
        JsonRpcMessage response = transport.sentMessages.get(0);
        assertNull(response.getError());
        assertInstanceOf(ToolCallResult.class, response.getResult());
        return (ToolCallResult) response.getResult();
    }

    static class MockTransport implements Transport {
        private Consumer<JsonRpcMessage> messageHandler;
        private boolean connected;
        private final List<JsonRpcMessage> sentMessages = new ArrayList<>();

        @Override
        public void start() {
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
        }

        @Override
        public void onClose(Runnable handler) {
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public void close() throws IOException {
            connected = false;
        }

        private void simulateIncoming(JsonRpcMessage message) {
            messageHandler.accept(message);
        }
    }
}
