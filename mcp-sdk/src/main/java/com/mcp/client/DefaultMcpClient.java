package com.mcp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.exception.McpException;
import com.mcp.protocol.*;
import com.mcp.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MCP 客户端默认实现
 * <p>
 * 基于 JSON-RPC 2.0 协议的 MCP 客户端完整实现，负责与 MCP 服务器建立连接，
 * 并通过异步请求-响应模式调用服务器提供的工具、资源和 Prompt 能力。
 * </p>
 * <p>
 * 核心职责：
 * <ul>
 *   <li>管理与服务器的传输层连接生命周期</li>
 *   <li>构造 JSON-RPC 请求并通过传输层发送</li>
 *   <li>通过 {@link CompletableFuture} 实现异步请求-响应匹配</li>
 *   <li>处理传输层的错误和关闭事件，清理等待中的请求</li>
 * </ul>
 * </p>
 * <p>
 * 支持通过 Builder 模式创建实例：
 * <pre>
 * DefaultMcpClient client = DefaultMcpClient.builder()
 *         .name("my-client").version("1.0.0").timeout(60).build();
 * </pre>
 * </p>
 *
 * @see McpClient
 */
public class DefaultMcpClient implements McpClient {

    /** 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(DefaultMcpClient.class);

    /** 默认请求超时时间（秒） */
    private static final long DEFAULT_TIMEOUT_SECONDS = 30;

    /** 客户端名称，用于初始化握手时标识客户端身份 */
    private final String clientName;

    /** 客户端版本号，用于初始化握手时标识客户端版本 */
    private final String clientVersion;

    /** JSON 序列化/反序列化器，用于处理 JSON-RPC 消息中的参数转换 */
    private final ObjectMapper objectMapper;

    /** 请求超时时间（秒），超过此时间未收到响应则抛出超时异常 */
    private final long timeoutSeconds;

    /** 传输层实例，负责与服务器的底层通信 */
    private Transport transport;

    /** 请求 ID 原子计数器，线程安全地生成唯一的 JSON-RPC 请求标识 */
    private final AtomicLong requestIdCounter = new AtomicLong(0);

    /** 等待响应的请求映射表，key 为请求 ID，value 为对应的 Future（用于异步匹配响应） */
    private final Map<Object, CompletableFuture<JsonRpcMessage>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * 使用默认超时时间创建 DefaultMcpClient 实例
     *
     * @param clientName    客户端名称
     * @param clientVersion 客户端版本号
     */
    public DefaultMcpClient(String clientName, String clientVersion) {
        this(clientName, clientVersion, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 创建 DefaultMcpClient 实例
     *
     * @param clientName    客户端名称
     * @param clientVersion 客户端版本号
     * @param timeoutSeconds 请求超时时间（秒）
     */
    public DefaultMcpClient(String clientName, String clientVersion, long timeoutSeconds) {
        this.clientName = clientName;
        this.clientVersion = clientVersion;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * {@inheritDoc}
     * <p>
     * 注册消息处理、错误处理和连接关闭的回调，然后启动传输层。
     * </p>
     */
    @Override
    public void connect(Transport transport) throws Exception {
        this.transport = transport;

        transport.onMessage(this::handleMessage);
        transport.onError(this::handleError);
        transport.onClose(this::handleClose);

        transport.start();
        log.info("Connected to MCP Server");
    }

    /**
     * {@inheritDoc}
     * <p>
     * 关闭传输层连接并清空所有等待中的请求。
     * </p>
     */
    @Override
    public void disconnect() throws Exception {
        if (transport != null) {
            transport.close();
        }
        pendingRequests.clear();
        log.info("Disconnected from MCP Server");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InitializeResult initialize(InitializeRequest request) throws Exception {
        JsonRpcMessage response = sendRequest(McpMethods.INITIALIZE, request);
        return objectMapper.convertValue(response.getResult(), InitializeResult.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Tool> listTools() throws Exception {
        JsonRpcMessage response = sendRequest(McpMethods.TOOLS_LIST, null);
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        List<Map<String, Object>> toolsList = (List<Map<String, Object>>) result.get("tools");
        return toolsList.stream()
                .map(map -> objectMapper.convertValue(map, Tool.class))
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ToolCallResult callTool(String name, Map<String, Object> arguments) throws Exception {
        ToolCallRequest request = new ToolCallRequest(name, arguments);
        JsonRpcMessage response = sendRequest(McpMethods.TOOLS_CALL, request);
        return objectMapper.convertValue(response.getResult(), ToolCallResult.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Resource> listResources() throws Exception {
        JsonRpcMessage response = sendRequest(McpMethods.RESOURCES_LIST, null);
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        List<Map<String, Object>> resourcesList = (List<Map<String, Object>>) result.get("resources");
        return resourcesList.stream()
                .map(map -> objectMapper.convertValue(map, Resource.class))
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ResourceContent> readResource(String uri) throws Exception {
        JsonRpcMessage response = sendRequest(McpMethods.RESOURCES_READ, Map.of("uri", uri));
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        List<Map<String, Object>> contentsList = (List<Map<String, Object>>) result.get("contents");
        return contentsList.stream()
                .map(map -> objectMapper.convertValue(map, ResourceContent.class))
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Prompt> listPrompts() throws Exception {
        JsonRpcMessage response = sendRequest(McpMethods.PROMPTS_LIST, null);
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        List<Map<String, Object>> promptsList = (List<Map<String, Object>>) result.get("prompts");
        return promptsList.stream()
                .map(map -> objectMapper.convertValue(map, Prompt.class))
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PromptResult getPrompt(String name, Map<String, String> arguments) throws Exception {
        Map<String, Object> params = Map.of("name", name, "arguments", arguments);
        JsonRpcMessage response = sendRequest(McpMethods.PROMPTS_GET, params);
        return objectMapper.convertValue(response.getResult(), PromptResult.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConnected() {
        return transport != null && transport.isConnected();
    }

    /**
     * 发送 JSON-RPC 请求并同步等待响应
     * <p>
     * 生成唯一的请求 ID，构造 JSON-RPC 请求消息，通过传输层发送，
     * 并使用 {@link CompletableFuture} 阻塞等待服务器响应。
     * 超时或异常时抛出 {@link McpException}。
     * </p>
     *
     * @param method 请求方法名（如 {@code "initialize"}、{@code "tools/list"} 等）
     * @param params 请求参数（可为 {@code null}）
     * @return 服务器返回的 JSON-RPC 响应消息
     * @throws McpException 未连接、超时或请求失败时抛出
     */
    private JsonRpcMessage sendRequest(String method, Object params) throws Exception {
        if (!isConnected()) {
            throw new IllegalStateException("Client is not connected");
        }

        Object id = requestIdCounter.incrementAndGet();
        JsonRpcMessage request = JsonRpcMessage.request(id, method, params);

        CompletableFuture<JsonRpcMessage> future = new CompletableFuture<>();
        pendingRequests.put(id, future);

        try {
            transport.send(request);
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            pendingRequests.remove(id);
            throw new McpException("Request failed: " + e.getMessage(), e);
        }
    }

    /**
     * 处理从传输层接收到的 JSON-RPC 消息
     * <p>
     * 根据消息类型分发处理：
     * <ul>
     *   <li>响应消息：从待处理请求中找到对应的 Future 并完成</li>
     *   <li>通知消息：转发到通知处理方法</li>
     * </ul>
     * </p>
     *
     * @param message 接收到的 JSON-RPC 消息
     */
    private void handleMessage(JsonRpcMessage message) {
        if (message.isResponse()) {
            CompletableFuture<JsonRpcMessage> future = pendingRequests.remove(message.getId());
            if (future != null) {
                if (message.getError() != null) {
                    future.completeExceptionally(
                            new McpException(message.getError().getCode(), message.getError().getMessage())
                    );
                } else {
                    future.complete(message);
                }
            }
        } else if (message.isNotification()) {
            handleNotification(message);
        }
    }

    /**
     * 处理服务器推送的通知消息
     * <p>
     * 当前仅记录日志，后续可扩展处理如资源变更、工具列表变更等通知。
     * </p>
     *
     * @param notification 通知消息
     */
    private void handleNotification(JsonRpcMessage notification) {
        log.debug("Received notification: {}", notification.getMethod());
        // 处理服务器推送的通知
    }

    /**
     * 传输层错误回调
     * <p>
     * 将错误传播给所有等待中的请求 Future，并清空待处理队列。
     * </p>
     *
     * @param error 错误信息
     */
    private void handleError(Throwable error) {
        log.error("Transport error", error);
        // 完成所有等待中的请求
        pendingRequests.forEach((id, future) -> {
            future.completeExceptionally(error);
        });
        pendingRequests.clear();
    }

    /**
     * 传输层关闭回调
     * <p>
     * 以 "Connection closed" 异常完成所有等待中的请求 Future，并清空待处理队列。
     * </p>
     */
    private void handleClose() {
        log.info("Transport closed");
        // 完成所有等待中的请求
        pendingRequests.forEach((id, future) -> {
            future.completeExceptionally(new McpException("Connection closed"));
        });
        pendingRequests.clear();
    }

    /**
     * 创建 DefaultMcpClient 的 Builder 实例
     *
     * @return 新的 Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * DefaultMcpClient 构建器
     * <p>
     * 支持链式调用设置客户端名称、版本号和超时时间，
     * 最终通过 {@link #build()} 创建实例。
     * </p>
     */
    public static class Builder {
        /** 客户端名称，默认为 "mcp-client" */
        private String name = "mcp-client";
        /** 客户端版本号，默认为 "1.0.0" */
        private String version = "1.0.0";
        /** 请求超时时间（秒），默认为 30 秒 */
        private long timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

        /**
         * 设置客户端名称
         *
         * @param name 客户端名称
         * @return 当前 Builder 实例
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 设置客户端版本号
         *
         * @param version 版本号
         * @return 当前 Builder 实例
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * 设置请求超时时间
         *
         * @param seconds 超时时间（秒）
         * @return 当前 Builder 实例
         */
        public Builder timeout(long seconds) {
            this.timeoutSeconds = seconds;
            return this;
        }

        /**
         * 构建 DefaultMcpClient 实例
         *
         * @return 配置好的 DefaultMcpClient 实例
         */
        public DefaultMcpClient build() {
            return new DefaultMcpClient(name, version, timeoutSeconds);
        }
    }
}
