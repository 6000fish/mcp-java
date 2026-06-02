package com.mcp.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.protocol.JsonRpcMessage;
import com.mcp.protocol.JsonUtils;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SSE 传输层单元测试
 *
 * 测试目的：验证 SseTransport 的核心功能，包括：
 * 1. HTTP 服务器启动和关闭
 * 2. SSE 连接建立和 endpoint 事件
 * 3. 消息发送和接收
 * 4. 会话管理
 *
 * 注意：使用随机端口避免端口冲突
 */
class SseTransportTest {

    private static final ObjectMapper mapper = JsonUtils.mapper();
    private SseTransport transport;

    @BeforeEach
    void setUp() {
        // 使用端口 0 让操作系统分配空闲端口
        transport = new SseTransport(0);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (transport != null) {
            transport.close();
        }
    }

    /**
     * 测试传输层启动和关闭
     *
     * 验证：
     * 1. 启动后 isConnected() 返回 true
     * 2. 关闭后 isConnected() 返回 false
     * 3. 可以正常启动和关闭
     */
    @Test
    void testStartAndClose() throws Exception {
        assertFalse(transport.isConnected());

        transport.start();
        assertTrue(transport.isConnected());

        transport.close();
        assertFalse(transport.isConnected());
    }

    /**
     * 测试重复启动的幂等性
     *
     * 验证多次调用 start() 不会抛出异常
     */
    @Test
    void testStartIdempotent() throws Exception {
        transport.start();
        transport.start(); // 不应抛出异常
        assertTrue(transport.isConnected());
    }

    /**
     * 测试重复关闭的幂等性
     *
     * 验证多次调用 close() 不会抛出异常
     */
    @Test
    void testCloseIdempotent() throws Exception {
        transport.start();
        transport.close();
        transport.close(); // 不应抛出异常
        assertFalse(transport.isConnected());
    }

    /**
     * 测试 SSE 端点返回正确的响应头
     *
     * 验证：
     * 1. Content-Type 为 text/event-stream
     * 2. 响应码为 200
     */
    @Test
    void testSseEndpoint() throws Exception {
        transport.start();

        URL url = new URL("http://localhost:" + transport.getPort() + "/sse");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(2000);

        assertEquals(200, conn.getResponseCode());
        assertEquals("text/event-stream", conn.getHeaderField("Content-Type"));

        conn.disconnect();
    }

    /**
     * 测试消息端点接受 POST 请求
     *
     * 验证：
     * 1. 无效 sessionId 返回 400
     * 2. 有效 sessionId 接受消息
     */
    @Test
    void testMessageEndpointInvalidSession() throws Exception {
        transport.start();

        URL url = new URL("http://localhost:" + transport.getPort() + "/messages?sessionId=invalid");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(400, conn.getResponseCode());
        conn.disconnect();
    }

    /**
     * 测试消息端点拒绝非 POST 请求
     */
    @Test
    void testMessageEndpointRejectsGet() throws Exception {
        transport.start();

        URL url = new URL("http://localhost:" + transport.getPort() + "/messages");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        assertEquals(405, conn.getResponseCode());
        conn.disconnect();
    }

    /**
     * 测试 SSE 端点拒绝非 GET 请求
     */
    @Test
    void testSseEndpointRejectsPost() throws Exception {
        transport.start();

        URL url = new URL("http://localhost:" + transport.getPort() + "/sse");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");

        assertEquals(405, conn.getResponseCode());
        conn.disconnect();
    }

    /**
     * 测试完整的消息收发流程
     *
     * 验证：
     * 1. 建立 SSE 连接
     * 2. 接收 endpoint 事件
     * 3. POST 消息到 endpoint
     * 4. 消息回调被触发
     */
    @Test
    void testFullMessageFlow() throws Exception {
        CompletableFuture<JsonRpcMessage> received = new CompletableFuture<>();
        transport.onMessage(msg -> received.complete(msg));
        transport.start();

        // 建立 SSE 连接，读取 endpoint 事件
        URL sseUrl = new URL("http://localhost:" + transport.getPort() + "/sse");
        HttpURLConnection sseConn = (HttpURLConnection) sseUrl.openConnection();
        sseConn.setRequestMethod("GET");
        sseConn.setConnectTimeout(2000);
        sseConn.setReadTimeout(5000);

        // 逐行读取 SSE 事件
        BufferedReader reader = new BufferedReader(new InputStreamReader(sseConn.getInputStream(), StandardCharsets.UTF_8));

        // 读取 event 行
        String eventLine = reader.readLine();
        assertNotNull(eventLine);
        assertEquals("event: endpoint", eventLine);

        // 读取 data 行
        String dataLine = reader.readLine();
        assertNotNull(dataLine);
        assertTrue(dataLine.startsWith("data: "));

        // 提取 endpoint
        String endpoint = dataLine.replace("data: ", "").trim();
        assertTrue(endpoint.contains("sessionId="));

        // 跳过空行
        reader.readLine();

        // POST 消息
        URL msgUrl = new URL("http://localhost:" + transport.getPort() + endpoint);
        HttpURLConnection msgConn = (HttpURLConnection) msgUrl.openConnection();
        msgConn.setRequestMethod("POST");
        msgConn.setDoOutput(true);
        msgConn.setRequestProperty("Content-Type", "application/json");

        String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}";
        try (OutputStream os = msgConn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(202, msgConn.getResponseCode());
        msgConn.disconnect();

        // 验证消息被接收
        JsonRpcMessage receivedMsg = received.get(5, TimeUnit.SECONDS);
        assertNotNull(receivedMsg);
        assertEquals("ping", receivedMsg.getMethod());
        assertEquals(1, receivedMsg.getId());

        sseConn.disconnect();
    }

    /**
     * 测试未连接时发送消息抛出异常
     */
    @Test
    void testSendWhenNotConnected() {
        JsonRpcMessage msg = JsonRpcMessage.notification("test", null);
        assertThrows(IllegalStateException.class, () -> transport.send(msg));
    }
}
