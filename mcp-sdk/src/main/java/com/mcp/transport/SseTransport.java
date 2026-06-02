package com.mcp.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.protocol.JsonRpcMessage;
import com.mcp.protocol.JsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * SSE (Server-Sent Events) 传输实现
 *
 * 基于 JDK 内置 HttpServer，通过 HTTP SSE 协议进行通信。
 * MCP SSE 协议流程：
 * 1. 客户端 GET /sse 建立 SSE 连接
 * 2. 服务端发送 endpoint 事件，告知 POST 地址
 * 3. 客户端 POST JSON-RPC 消息到该地址
 * 4. 服务端通过 SSE 流推送响应
 *
 * @author MCP Java SDK
 */
public class SseTransport implements Transport {

    private static final Logger log = LoggerFactory.getLogger(SseTransport.class);

    /** 监听端口 */
    private final int port;

    /** SSE 端点路径 */
    private final String ssePath;

    /** 消息接收端点路径 */
    private final String messagePath;

    /** JDK 内置 HTTP 服务器 */
    private HttpServer httpServer;

    /** JSON 序列化器 */
    private final ObjectMapper objectMapper;

    /** 活跃的 SSE 会话 */
    private final Map<String, SseSession> sessions = new ConcurrentHashMap<>();

    /** 连接状态 */
    private final AtomicBoolean connected = new AtomicBoolean(false);

    /** 消息回调 */
    private Consumer<JsonRpcMessage> messageHandler;

    /** 错误回调 */
    private Consumer<Throwable> errorHandler;

    /** 关闭回调 */
    private Runnable closeHandler;

    /**
     * 使用默认端口和路径创建 SSE 传输
     */
    public SseTransport() {
        this(8080, "/sse", "/messages");
    }

    /**
     * 使用指定端口创建 SSE 传输
     *
     * @param port 监听端口
     */
    public SseTransport(int port) {
        this(port, "/sse", "/messages");
    }

    /**
     * 使用指定端口和路径创建 SSE 传输
     *
     * @param port       监听端口
     * @param ssePath    SSE 端点路径
     * @param messagePath 消息接收端点路径
     */
    public SseTransport(int port, String ssePath, String messagePath) {
        this.port = port;
        this.ssePath = ssePath;
        this.messagePath = messagePath;
        this.objectMapper = JsonUtils.mapper();
    }

    /**
     * 获取实际监听端口
     *
     * @return 端口号
     */
    public int getPort() {
        if (httpServer != null) {
            return httpServer.getAddress().getPort();
        }
        return port;
    }

    @Override
    public void start() throws Exception {
        if (connected.compareAndSet(false, true)) {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);

            // 注册 SSE 端点
            httpServer.createContext(ssePath, this::handleSse);

            // 注册消息接收端点
            httpServer.createContext(messagePath, this::handleMessage);

            // 使用线程池处理请求
            httpServer.setExecutor(Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "mcp-sse-worker");
                t.setDaemon(true);
                return t;
            }));

            httpServer.start();
            log.info("SSE Transport started on port {}, SSE endpoint: {}, message endpoint: {}",
                    port, ssePath, messagePath);
        }
    }

    @Override
    public void send(JsonRpcMessage message) throws Exception {
        if (!connected.get()) {
            throw new IllegalStateException("Transport is not connected");
        }

        String json = objectMapper.writeValueAsString(message);
        String sseEvent = "event: message\ndata: " + json + "\n\n";

        // 向所有活跃会话广播
        for (Map.Entry<String, SseSession> entry : sessions.entrySet()) {
            SseSession session = entry.getValue();
            if (session.isActive()) {
                try {
                    synchronized (session.outputStream) {
                        session.outputStream.write(sseEvent.getBytes(StandardCharsets.UTF_8));
                        session.outputStream.flush();
                    }
                    log.debug("Sent message to session {}: {}", session.sessionId, json);
                } catch (IOException e) {
                    log.warn("Failed to send to session {}, removing: {}", session.sessionId, e.getMessage());
                    session.deactivate();
                    sessions.remove(entry.getKey());
                }
            }
        }
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
        return connected.get();
    }

    @Override
    public void close() throws IOException {
        if (connected.compareAndSet(true, false)) {
            log.info("Closing SSE Transport");

            // 关闭所有活跃会话
            for (SseSession session : sessions.values()) {
                session.deactivate();
            }
            sessions.clear();

            // 关闭 HTTP 服务器
            if (httpServer != null) {
                httpServer.stop(0);
            }

            if (closeHandler != null) {
                closeHandler.run();
            }
        }
    }

    /**
     * 处理 SSE 连接请求
     *
     * 客户端通过 GET /sse 建立 SSE 连接，
     * 服务端发送 endpoint 事件告知客户端 POST 地址
     */
    private void handleSse(HttpExchange exchange) throws IOException {
        // 仅接受 GET 请求
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        // 生成会话 ID
        String sessionId = UUID.randomUUID().toString();

        // 设置 SSE 响应头
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        // 发送响应头，chunked 传输
        exchange.sendResponseHeaders(200, 0);

        OutputStream outputStream = exchange.getResponseBody();
        SseSession session = new SseSession(sessionId, outputStream);
        sessions.put(sessionId, session);

        log.info("SSE session established: {}", sessionId);

        // 发送 endpoint 事件，告知客户端 POST 地址
        String endpoint = messagePath + "?sessionId=" + sessionId;
        String endpointEvent = "event: endpoint\ndata: " + endpoint + "\n\n";
        synchronized (outputStream) {
            outputStream.write(endpointEvent.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        }

        // 保持连接直到客户端断开
        try {
            // 阻塞等待客户端断开（通过检测连接状态）
            while (connected.get() && session.isActive()) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            session.deactivate();
            sessions.remove(sessionId);
            log.info("SSE session closed: {}", sessionId);
        }
    }

    /**
     * 处理消息接收请求
     *
     * 客户端通过 POST /messages?sessionId=xxx 发送 JSON-RPC 消息
     */
    private void handleMessage(HttpExchange exchange) throws IOException {
        // 仅接受 POST 请求
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        // 获取 sessionId
        String query = exchange.getRequestURI().getQuery();
        String sessionId = extractSessionId(query);

        if (sessionId == null || !sessions.containsKey(sessionId)) {
            String error = "{\"error\": \"Invalid or missing sessionId\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, error.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(error.getBytes(StandardCharsets.UTF_8));
            exchange.getResponseBody().close();
            return;
        }

        // 读取请求体
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        log.debug("Received message from session {}: {}", sessionId, body);

        // 解析 JSON-RPC 消息
        try {
            JsonRpcMessage message = objectMapper.readValue(body, JsonRpcMessage.class);
            if (messageHandler != null) {
                messageHandler.accept(message);
            }

            // 返回 202 Accepted
            exchange.sendResponseHeaders(202, -1);
        } catch (Exception e) {
            log.error("Failed to parse message: {}", body, e);
            if (errorHandler != null) {
                errorHandler.accept(e);
            }

            String error = "{\"error\": \"Invalid JSON-RPC message\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, error.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(error.getBytes(StandardCharsets.UTF_8));
        } finally {
            exchange.getResponseBody().close();
        }
    }

    /**
     * 从查询字符串中提取 sessionId
     */
    private String extractSessionId(String query) {
        if (query == null) {
            return null;
        }
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "sessionId".equals(kv[0])) {
                return kv[1];
            }
        }
        return null;
    }

    /**
     * SSE 会话
     *
     * 代表一个客户端 SSE 连接
     */
    private static class SseSession {
        private final String sessionId;
        private final OutputStream outputStream;
        private volatile boolean active;

        SseSession(String sessionId, OutputStream outputStream) {
            this.sessionId = sessionId;
            this.outputStream = outputStream;
            this.active = true;
        }

        boolean isActive() {
            return active;
        }

        void deactivate() {
            this.active = false;
            try {
                outputStream.close();
            } catch (IOException ignored) {
            }
        }
    }
}
