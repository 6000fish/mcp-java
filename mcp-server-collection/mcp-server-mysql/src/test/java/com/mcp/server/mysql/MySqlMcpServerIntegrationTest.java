package com.mcp.server.mysql;

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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MySQL 真实环境集成测试
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MySqlMcpServerIntegrationTest {

    private final Properties config = new Properties();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MySqlMcpServer mysqlServer;
    private MockTransport transport;
    private String host;
    private int port;
    private String database;
    private String jdbcUrl;
    private String username;
    private String password;

    /**
     * 加载本地 MySQL 与可选大模型配置，并初始化测试数据库和 MCP Server 实例。
     */
    @BeforeAll
    void loadConfig() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("integration-test.properties")) {
            Assumptions.assumeTrue(input != null, "缺少 integration-test.properties，跳过 MySQL 集成测试");
            config.load(input);
        }

        host = config.getProperty("mysql.host", "localhost");
        port = Integer.parseInt(config.getProperty("mysql.port", "3306"));
        database = config.getProperty("mysql.database", "mcp_test");
        username = config.getProperty("mysql.username", "root");
        password = config.getProperty("mysql.password", "");
        jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true", host, port, database);

        prepareDatabase();
        mysqlServer = new MySqlMcpServer(host, port, database, username, password);
    }

    /**
     * 每个测试用例前创建新的 MCP Server，并通过注解扫描注册 MySQL 工具。
     */
    @BeforeEach
    void setUp() throws Exception {
        McpServer server = DefaultMcpServer.builder()
                .name("mysql-it-server")
                .version("1.0.0")
                .build();
        McpAnnotationScanner.scan(server, mysqlServer);

        transport = new MockTransport();
        server.start(transport);
    }

    /**
     * 测试结束后关闭 MySQL 连接池，释放真实数据库连接。
     */
    @AfterAll
    void tearDown() {
        if (mysqlServer != null) {
            mysqlServer.close();
        }
    }

    /**
     * 验证模型发起查询类工具调用时，MCP 能通过真实 MySQL 返回目标用户数据。
     */
    @Test
    void testModelCanQueryMysqlThroughMcp() {
        ToolCallResult result = callTool("query", Map.of(
                "sql", "SELECT name, age FROM mcp_integration_users WHERE name = 'Alice'"
        ));

        assertFalse(result.getIsError());
        String text = result.getContent().get(0).getText();
        assertTrue(text.contains("Alice"));
        assertTrue(text.contains("28"));
    }

    /**
     * 验证模型发起写入后再查询时，MCP 能通过真实 MySQL 完成数据闭环。
     */
    @Test
    void testModelCanInsertAndQueryMysqlThroughMcp() {
        ToolCallResult insertResult = callTool("execute", Map.of(
                "sql", "INSERT INTO mcp_integration_users(name, age) VALUES ('Bob', 35)"
        ));
        assertFalse(insertResult.getIsError());
        assertTrue(insertResult.getContent().get(0).getText().contains("Rows affected: 1"));

        ToolCallResult queryResult = callTool("query", Map.of(
                "sql", "SELECT name, age FROM mcp_integration_users WHERE name = 'Bob'"
        ));
        assertFalse(queryResult.getIsError());
        assertTrue(queryResult.getContent().get(0).getText().contains("Bob"));
        assertTrue(queryResult.getContent().get(0).getText().contains("35"));
    }

    /**
     * 验证模型发起结构探查类工具调用时，MCP 能读取真实 MySQL 表清单、表结构和执行计划。
     */
    @Test
    void testModelCanInspectMysqlSchemaThroughMcp() {
        ToolCallResult tablesResult = callTool("list_tables", Map.of());
        assertFalse(tablesResult.getIsError());
        assertTrue(tablesResult.getContent().get(0).getText().contains("mcp_integration_users"));

        ToolCallResult describeResult = callTool("describe_table", Map.of("table", "mcp_integration_users"));
        assertFalse(describeResult.getIsError());
        assertTrue(describeResult.getContent().get(0).getText().contains("name"));
        assertTrue(describeResult.getContent().get(0).getText().contains("age"));

        ToolCallResult explainResult = callTool("explain_query", Map.of(
                "sql", "SELECT * FROM mcp_integration_users WHERE age > 20"
        ));
        assertFalse(explainResult.getIsError());
        assertTrue(explainResult.getContent().get(0).getText().contains("mcp_integration_users"));
    }

    /**
     * 验证自然语言插入请求经大模型选择工具后，MCP 能执行写入并让模型生成最终回复。
     */
    @Test
    void testModelCanChooseMysqlToolFromConversation() throws Exception {
        Assumptions.assumeTrue(isModelConfigured(), "缺少 LLM 配置，跳过大模型驱动的 MySQL 集成测试");
        String userMessage = "帮我在 mcp_integration_users 表中插入10条测试数据。";

        Map<String, Object> decision = askModelForToolCall(userMessage, """
                可用 MCP 工具：
                - execute: 参数 {"sql":"INSERT 语句"}，用于向 MySQL 写入数据。
                请只返回 JSON：{"tool":"execute","arguments":{"sql":"..."}}
                SQL 只能向 mcp_integration_users 表插入数据。
                必须使用一条 INSERT INTO mcp_integration_users(name, age) VALUES ... 语句一次插入 10 行。
                测试数据的 name 使用 AgentUser1 到 AgentUser10，age 使用 21 到 30。
                """);

        assertEquals("execute", decision.get("tool"));
        ToolCallResult insertResult = callTool((String) decision.get("tool"), castArguments(decision.get("arguments")));

        assertFalse(insertResult.getIsError());
        String insertText = insertResult.getContent().get(0).getText();
        assertTrue(insertText.contains("Rows affected: 10"));

        ToolCallResult queryResult = callTool("query", Map.of(
                "sql", "SELECT name, age FROM mcp_integration_users WHERE name LIKE 'AgentUser%' ORDER BY age"
        ));
        assertFalse(queryResult.getIsError());
        String queryText = queryResult.getContent().get(0).getText();
        assertTrue(queryText.contains("AgentUser1"));
        assertTrue(queryText.contains("AgentUser10"));

        String finalAnswer = askModelForFinalAnswer(userMessage, insertText + "\n" + queryText);
        System.out.println("Agent final answer: " + finalAnswer);
        assertTrue(finalAnswer.contains("10") || finalAnswer.contains("十"));
    }

    /**
     * 创建测试数据库和测试表，确保集成测试拥有可重复的 MySQL 初始数据。
     */
    private void prepareDatabase() throws Exception {
        String rootJdbcUrl = String.format("jdbc:mysql://%s:%d/?useSSL=false&allowPublicKeyRetrieval=true", host, port);
        try (Connection conn = DriverManager.getConnection(rootJdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + database);
        }

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TABLE IF EXISTS mcp_integration_users");
            stmt.executeUpdate("CREATE TABLE mcp_integration_users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(64) NOT NULL," +
                    "age INT NOT NULL" +
                    ")");
            stmt.executeUpdate("INSERT INTO mcp_integration_users(name, age) VALUES ('Alice', 28)");
        }
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
     * 调用 OpenAI 或 Anthropic 兼容的大模型接口，让模型基于自然语言和工具说明生成 MCP 工具调用 JSON。
     */
    private Map<String, Object> askModelForToolCall(String userMessage, String systemPrompt) throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(buildModelRequest(userMessage, systemPrompt), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());

        String content = modelResponseText(response.body());
        return objectMapper.readValue(extractJson(content), new TypeReference<>() {});
    }

    /**
     * 调用大模型，让模型基于 MCP 工具执行结果生成面向用户的最终回复。
     */
    private String askModelForFinalAnswer(String userMessage, String toolResult) throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(buildModelRequest(userMessage, """
                你是一个可以通过 MCP 工具操作 MySQL 的 Agent。
                MCP 工具已经执行完成，下面是工具返回结果。
                请基于工具结果，用中文简洁回复用户实际完成了什么，不要再输出工具调用 JSON。

                MCP 工具结果：
                """ + toolResult), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        return modelResponseText(response.body());
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
     * 模拟客户端通过 MCP tools/call 请求调用指定 MySQL 工具。
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
