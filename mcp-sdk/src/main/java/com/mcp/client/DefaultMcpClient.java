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
 * MCP Client 默认实现
 */
public class DefaultMcpClient implements McpClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultMcpClient.class);
    private static final long DEFAULT_TIMEOUT_SECONDS = 30;

    private final String clientName;
    private final String clientVersion;
    private final ObjectMapper objectMapper;
    private final long timeoutSeconds;

    private Transport transport;
    private final AtomicLong requestIdCounter = new AtomicLong(0);
    private final Map<Object, CompletableFuture<JsonRpcMessage>> pendingRequests = new ConcurrentHashMap<>();

    public DefaultMcpClient(String clientName, String clientVersion) {
        this(clientName, clientVersion, DEFAULT_TIMEOUT_SECONDS);
    }

    public DefaultMcpClient(String clientName, String clientVersion, long timeoutSeconds) {
        this.clientName = clientName;
        this.clientVersion = clientVersion;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void connect(Transport transport) throws Exception {
        this.transport = transport;

        transport.onMessage(this::handleMessage);
        transport.onError(this::handleError);
        transport.onClose(this::handleClose);

        transport.start();
        log.info("Connected to MCP Server");
    }

    @Override
    public void disconnect() throws Exception {
        if (transport != null) {
            transport.close();
        }
        pendingRequests.clear();
        log.info("Disconnected from MCP Server");
    }

    @Override
    public InitializeResult initialize(InitializeRequest request) throws Exception {
        JsonRpcMessage response = sendRequest(McpMethods.INITIALIZE, request);
        return objectMapper.convertValue(response.getResult(), InitializeResult.class);
    }

    @Override
    public List<Tool> listTools() throws Exception {
        JsonRpcMessage response = sendRequest(McpMethods.TOOLS_LIST, null);
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        List<Map<String, Object>> toolsList = (List<Map<String, Object>>) result.get("tools");
        return toolsList.stream()
                .map(map -> objectMapper.convertValue(map, Tool.class))
                .toList();
    }

    @Override
    public ToolCallResult callTool(String name, Map<String, Object> arguments) throws Exception {
        ToolCallRequest request = new ToolCallRequest(name, arguments);
        JsonRpcMessage response = sendRequest(McpMethods.TOOLS_CALL, request);
        return objectMapper.convertValue(response.getResult(), ToolCallResult.class);
    }

    @Override
    public List<Resource> listResources() throws Exception {
        JsonRpcMessage response = sendRequest(McpMethods.RESOURCES_LIST, null);
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        List<Map<String, Object>> resourcesList = (List<Map<String, Object>>) result.get("resources");
        return resourcesList.stream()
                .map(map -> objectMapper.convertValue(map, Resource.class))
                .toList();
    }

    @Override
    public List<ResourceContent> readResource(String uri) throws Exception {
        JsonRpcMessage response = sendRequest(McpMethods.RESOURCES_READ, Map.of("uri", uri));
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        List<Map<String, Object>> contentsList = (List<Map<String, Object>>) result.get("contents");
        return contentsList.stream()
                .map(map -> objectMapper.convertValue(map, ResourceContent.class))
                .toList();
    }

    @Override
    public List<Prompt> listPrompts() throws Exception {
        JsonRpcMessage response = sendRequest(McpMethods.PROMPTS_LIST, null);
        Map<String, Object> result = (Map<String, Object>) response.getResult();
        List<Map<String, Object>> promptsList = (List<Map<String, Object>>) result.get("prompts");
        return promptsList.stream()
                .map(map -> objectMapper.convertValue(map, Prompt.class))
                .toList();
    }

    @Override
    public PromptResult getPrompt(String name, Map<String, String> arguments) throws Exception {
        Map<String, Object> params = Map.of("name", name, "arguments", arguments);
        JsonRpcMessage response = sendRequest(McpMethods.PROMPTS_GET, params);
        return objectMapper.convertValue(response.getResult(), PromptResult.class);
    }

    @Override
    public boolean isConnected() {
        return transport != null && transport.isConnected();
    }

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

    private void handleNotification(JsonRpcMessage notification) {
        log.debug("Received notification: {}", notification.getMethod());
        // 处理服务器推送的通知
    }

    private void handleError(Throwable error) {
        log.error("Transport error", error);
        // 完成所有等待中的请求
        pendingRequests.forEach((id, future) -> {
            future.completeExceptionally(error);
        });
        pendingRequests.clear();
    }

    private void handleClose() {
        log.info("Transport closed");
        // 完成所有等待中的请求
        pendingRequests.forEach((id, future) -> {
            future.completeExceptionally(new McpException("Connection closed"));
        });
        pendingRequests.clear();
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder 类
     */
    public static class Builder {
        private String name = "mcp-client";
        private String version = "1.0.0";
        private long timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder timeout(long seconds) {
            this.timeoutSeconds = seconds;
            return this;
        }

        public DefaultMcpClient build() {
            return new DefaultMcpClient(name, version, timeoutSeconds);
        }
    }
}
