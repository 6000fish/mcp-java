package com.mcp.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.protocol.JsonRpcMessage;
import com.mcp.protocol.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * SSE (Server-Sent Events) 客户端传输层实现。
 * <p>
 * 该实现通过 HTTP SSE 协议连接到远程 MCP 服务器，适用于客户端-服务端分离的场景。
 * 与 {@link SseTransport}（服务端）不同，本类作为客户端角色，主动发起 SSE 连接并接收服务端推送的消息。
 * </p>
 * <p>
 * MCP SSE 客户端协议流程：
 * <ol>
 *   <li>客户端通过 GET 请求连接到 SSE 端点（如 {@code http://localhost:8080/sse}）</li>
 *   <li>服务端通过 SSE 流发送 {@code endpoint} 事件，告知客户端消息发送地址（如 {@code /messages?sessionId=xxx}）</li>
 *   <li>客户端通过 POST 请求将 JSON-RPC 消息发送到该地址</li>
 *   <li>服务端通过 SSE 流的 {@code message} 事件推送 JSON-RPC 响应</li>
 * </ol>
 * </p>
 * <p>
 * 实现细节：
 * <ul>
 *   <li>使用 {@link HttpURLConnection} 进行 HTTP 通信，无外部依赖</li>
 *   <li>SSE 流解析在独立的守护线程中异步执行，线程名为 {@code mcp-sse-client-reader}</li>
 *   <li>通过 {@link AtomicBoolean} 保证连接状态的线程安全</li>
 *   <li>使用 Jackson {@link ObjectMapper} 进行 JSON 序列化/反序列化</li>
 * </ul>
 * </p>
 *
 * @see Transport
 * @see SseTransport
 */
public class SseClientTransport implements Transport {

    /** 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(SseClientTransport.class);

    /** SSE 端点 URL，例如 "http://localhost:8080/sse" */
    private final String sseUrl;

    /** JSON 序列化/反序列化器 */
    private final ObjectMapper objectMapper;

    /** 连接状态标志，使用原子操作保证线程安全 */
    private final AtomicBoolean connected = new AtomicBoolean(false);

    /** 消息发送端点路径，由服务端通过 endpoint 事件告知，例如 "/messages?sessionId=xxx" */
    private volatile String messageEndpoint;

    /** 后台 SSE 流读取线程 */
    private Thread readerThread;

    /** SSE 流的输入流引用，用于关闭时释放资源 */
    private volatile InputStream sseInputStream;

    /** SSE 连接对象引用，用于关闭时断开连接 */
    private volatile HttpURLConnection sseConnection;

    /** 消息接收回调处理器 */
    private Consumer<JsonRpcMessage> messageHandler;

    /** 错误回调处理器 */
    private Consumer<Throwable> errorHandler;

    /** 连接关闭回调处理器 */
    private Runnable closeHandler;

    /**
     * 使用指定的 SSE 端点 URL 创建客户端传输实例。
     * <p>
     * 使用 {@link JsonUtils#mapper()} 获取共享的 ObjectMapper 实例。
     * </p>
     *
     * @param sseUrl SSE 端点 URL，例如 {@code "http://localhost:8080/sse"}，不能为 {@code null}
     */
    public SseClientTransport(String sseUrl) {
        this(sseUrl, JsonUtils.mapper());
    }

    /**
     * 使用指定的 SSE 端点 URL 和 ObjectMapper 创建客户端传输实例。
     *
     * @param sseUrl       SSE 端点 URL，例如 {@code "http://localhost:8080/sse"}，不能为 {@code null}
     * @param objectMapper JSON 序列化/反序列化器，不能为 {@code null}
     */
    public SseClientTransport(String sseUrl, ObjectMapper objectMapper) {
        this.sseUrl = sseUrl;
        this.objectMapper = objectMapper;
    }

    /**
     * 启动 SSE 客户端传输层，建立与远程 MCP 服务器的 SSE 连接。
     * <p>
     * 该方法执行以下操作：
     * <ol>
     *   <li>通过 GET 请求连接到 SSE 端点</li>
     *   <li>启动守护线程异步读取 SSE 事件流</li>
     *   <li>解析 {@code endpoint} 事件获取消息发送地址</li>
     *   <li>解析 {@code message} 事件并分发给消息处理器</li>
     * </ol>
     * </p>
     * <p>
     * 该方法是幂等的，如果传输层已经启动则不会重复启动。
     * </p>
     *
     * @throws Exception 启动过程中可能发生的异常（如连接失败、URL 格式错误等）
     */
    @Override
    public void start() throws Exception {
        if (connected.compareAndSet(false, true)) {
            log.info("Starting SSE client transport, connecting to: {}", sseUrl);
            startReaderThread();
        }
    }

    /**
     * 启动后台守护线程，建立 SSE 连接并持续读取事件流。
     * <p>
     * 线程执行流程：
     * <ol>
     *   <li>打开到 SSE 端点的 GET 连接，设置 Accept 头为 {@code text/event-stream}</li>
     *   <li>检查响应状态码，非 200 则抛出异常</li>
     *   <li>逐行读取 SSE 事件流，解析 {@code event:} 和 {@code data:} 行</li>
     *   <li>遇到空行时分发事件：{@code endpoint} 事件设置消息地址，{@code message} 事件反序列化并通知处理器</li>
     * </ol>
     * </p>
     * <p>
     * 当连接断开或调用 {@link #close()} 时，线程退出并触发关闭回调。
     * </p>
     */
    private void startReaderThread() {
        readerThread = new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                // 建立 SSE 连接
                URL url = new URL(sseUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "text/event-stream");
                connection.setRequestProperty("Cache-Control", "no-cache");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(0); // SSE 连接不设置读超时，保持长连接
                connection.setInstanceFollowRedirects(true);

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    throw new IOException("SSE connection failed with HTTP status: " + responseCode);
                }

                // 保存连接引用，用于关闭时释放资源
                sseConnection = connection;
                log.info("SSE connection established to: {}", sseUrl);

                // 读取 SSE 事件流
                try (InputStream is = connection.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                    // 保存输入流引用
                    sseInputStream = is;

                    // SSE 事件解析状态
                    String currentEvent = null;
                    StringBuilder currentData = new StringBuilder();

                    String line;
                    while (connected.get() && (line = reader.readLine()) != null) {
                        // 空行表示事件结束，分发事件
                        if (line.isEmpty()) {
                            if (currentEvent != null || currentData.length() > 0) {
                                dispatchEvent(currentEvent, currentData.toString().trim());
                                currentEvent = null;
                                currentData.setLength(0);
                            }
                            continue;
                        }

                        // 解析 event: 行
                        if (line.startsWith("event:")) {
                            currentEvent = line.substring("event:".length()).trim();
                        }
                        // 解析 data: 行（支持多行 data，用换行符连接）
                        else if (line.startsWith("data:")) {
                            if (currentData.length() > 0) {
                                currentData.append("\n");
                            }
                            currentData.append(line.substring("data:".length()).trim());
                        }
                        // 忽略以 : 开头的注释行和其他未知字段
                    }
                }
            } catch (Exception e) {
                if (connected.get()) {
                    log.error("SSE client reader error", e);
                    if (errorHandler != null) {
                        errorHandler.accept(e);
                    }
                }
            } finally {
                // 清理资源
                if (connection != null) {
                    connection.disconnect();
                }
                sseConnection = null;
                sseInputStream = null;
                connected.set(false);
                if (closeHandler != null) {
                    closeHandler.run();
                }
            }
        }, "mcp-sse-client-reader");

        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * 分发 SSE 事件到对应的处理器。
     * <p>
     * 支持的事件类型：
     * <ul>
     *   <li>{@code endpoint} - 服务端告知消息发送端点，设置 {@link #messageEndpoint}</li>
     *   <li>{@code message} - 服务端推送的 JSON-RPC 消息，反序列化后通知消息处理器</li>
     * </ul>
     * </p>
     *
     * @param event 事件类型，可能为 {@code null}
     * @param data  事件数据，已去除前缀和空白
     */
    private void dispatchEvent(String event, String data) {
        if (data.isEmpty()) {
            return;
        }

        log.debug("SSE event received - event: {}, data: {}", event, data);

        // endpoint 事件：服务端告知消息发送端点
        if ("endpoint".equals(event)) {
            messageEndpoint = data;
            log.info("Received message endpoint: {}", messageEndpoint);
            return;
        }

        // message 事件（或未指定事件类型时默认为消息）：服务端推送的 JSON-RPC 消息
        if (event == null || "message".equals(event)) {
            try {
                JsonRpcMessage message = objectMapper.readValue(data, JsonRpcMessage.class);
                if (messageHandler != null) {
                    messageHandler.accept(message);
                }
            } catch (Exception e) {
                log.error("Failed to parse SSE message: {}", data, e);
                if (errorHandler != null) {
                    errorHandler.accept(e);
                }
            }
        }
    }

    /**
     * 通过 HTTP POST 请求发送 JSON-RPC 消息到 MCP 服务端。
     * <p>
     * 消息发送目标地址由服务端在 {@code endpoint} 事件中指定。
     * 如果尚未接收到 {@code endpoint} 事件（即 {@link #messageEndpoint} 为 {@code null}），
     * 将抛出 {@link IllegalStateException}。
     * </p>
     * <p>
     * 发送流程：
     * <ol>
     *   <li>将 {@link JsonRpcMessage} 序列化为 JSON 字符串</li>
     *   <li>构造完整的消息发送 URL（基于 SSE URL 的基地址 + messageEndpoint）</li>
     *   <li>通过 POST 请求发送 JSON 数据</li>
     *   <li>检查响应状态码（期望 202 Accepted 或 200 OK）</li>
     * </ol>
     * </p>
     *
     * @param message 要发送的 JSON-RPC 消息，不能为 {@code null}
     * @throws IllegalStateException 如果传输层未连接或尚未接收到 endpoint 事件
     * @throws IOException          发送过程中可能发生的 I/O 异常
     */
    @Override
    public void send(JsonRpcMessage message) throws Exception {
        if (!connected.get()) {
            throw new IllegalStateException("Transport is not connected");
        }

        String endpoint = messageEndpoint;
        if (endpoint == null) {
            throw new IllegalStateException("Message endpoint not yet received from server");
        }

        // 序列化消息为 JSON
        String json = objectMapper.writeValueAsString(message);
        log.debug("Sending message: {}", json);

        // 构造完整的消息发送 URL
        // sseUrl 例如: http://localhost:8080/sse
        // endpoint 例如: /messages?sessionId=xxx
        // 完整 URL: http://localhost:8080/messages?sessionId=xxx
        URL sseBase = new URL(sseUrl);
        String baseUrl = sseBase.getProtocol() + "://" + sseBase.getHost()
                + (sseBase.getPort() != -1 ? ":" + sseBase.getPort() : "");
        URL messageUrl = new URL(baseUrl + endpoint);

        // 发送 POST 请求
        HttpURLConnection connection = (HttpURLConnection) messageUrl.openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            // 写入请求体
            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            // 检查响应状态码
            int responseCode = connection.getResponseCode();
            if (responseCode != 200 && responseCode != 202) {
                // 尝试读取错误响应体
                String errorBody = readErrorBody(connection);
                throw new IOException("Failed to send message, HTTP status: " + responseCode
                        + (errorBody.isEmpty() ? "" : ", response: " + errorBody));
            }

            log.debug("Message sent successfully, response code: {}", responseCode);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 读取 HTTP 连接的错误响应体。
     * <p>
     * 尝试从 {@link HttpURLConnection#getErrorStream()} 中读取响应内容，
     * 用于在请求失败时提供更详细的错误信息。如果读取失败则返回空字符串。
     * </p>
     *
     * @param connection HTTP 连接对象
     * @return 错误响应体内容，读取失败时返回空字符串
     */
    private String readErrorBody(HttpURLConnection connection) {
        try (InputStream errorStream = connection.getErrorStream()) {
            if (errorStream == null) {
                return "";
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 注册消息接收回调处理器。
     * <p>
     * 当从 SSE 流中接收到 {@code message} 事件时，会将解析后的 {@link JsonRpcMessage}
     * 传递给此处理器。每次调用会覆盖之前设置的处理器。
     * </p>
     *
     * @param handler 消息处理器，接收 {@link JsonRpcMessage} 作为参数
     */
    @Override
    public void onMessage(Consumer<JsonRpcMessage> handler) {
        this.messageHandler = handler;
    }

    /**
     * 注册错误回调处理器。
     * <p>
     * 当传输层发生异常（如 SSE 连接中断、消息解析失败、HTTP 请求错误等）时，
     * 将调用此回调。每次调用会覆盖之前设置的处理器。
     * </p>
     *
     * @param handler 错误处理器，接收 {@link Throwable} 作为参数
     */
    @Override
    public void onError(Consumer<Throwable> handler) {
        this.errorHandler = handler;
    }

    /**
     * 注册连接关闭回调处理器。
     * <p>
     * 当 SSE 连接关闭时（无论是正常关闭还是异常断开），将调用此回调。
     * 每次调用会覆盖之前设置的处理器。
     * </p>
     *
     * @param handler 关闭回调，无参数的 {@link Runnable}
     */
    @Override
    public void onClose(Runnable handler) {
        this.closeHandler = handler;
    }

    /**
     * 判断当前传输层是否处于已连接状态。
     * <p>
     * 注意：返回 {@code true} 仅表示 {@link #start()} 已被调用且连接未被显式关闭，
     * 并不保证 SSE 流仍然活跃（底层 TCP 连接可能已断开但尚未被检测到）。
     * </p>
     *
     * @return 已连接返回 {@code true}，否则返回 {@code false}
     */
    @Override
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * 关闭 SSE 客户端传输层，释放所有资源。
     * <p>
     * 该方法执行以下操作：
     * <ol>
     *   <li>将连接状态设置为 {@code false}，通知读取线程退出</li>
     *   <li>关闭 SSE 输入流以中断阻塞的读取操作</li>
     *   <li>断开 SSE HTTP 连接</li>
     *   <li>中断读取线程（如果仍在运行）</li>
     * </ol>
     * </p>
     * <p>
     * 该方法是幂等的，重复调用安全。关闭后可通过再次调用 {@link #start()} 重新建立连接。
     * </p>
     *
     * @throws IOException 关闭过程中可能发生的 I/O 异常
     */
    @Override
    public void close() throws IOException {
        if (connected.compareAndSet(true, false)) {
            log.info("Closing SSE client transport");

            // 关闭输入流以中断阻塞的读取操作
            InputStream is = sseInputStream;
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.debug("Error closing SSE input stream: {}", e.getMessage());
                }
            }

            // 断开 HTTP 连接
            HttpURLConnection conn = sseConnection;
            if (conn != null) {
                conn.disconnect();
            }

            // 中断读取线程
            Thread thread = readerThread;
            if (thread != null) {
                thread.interrupt();
            }

            // 清理引用
            messageEndpoint = null;
            sseInputStream = null;
            sseConnection = null;
            readerThread = null;
        }
    }
}
