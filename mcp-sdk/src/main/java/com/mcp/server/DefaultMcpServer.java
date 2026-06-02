package com.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.exception.McpException;
import com.mcp.protocol.*;
import com.mcp.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Server 默认实现
 */
public class DefaultMcpServer implements McpServer {

    private static final Logger log = LoggerFactory.getLogger(DefaultMcpServer.class);
    private static final String PROTOCOL_VERSION = "2024-11-05";

    private final String name;
    private final String version;
    private final ObjectMapper objectMapper;

    private final Map<String, ToolEntry> tools = new ConcurrentHashMap<>();
    private final Map<String, ResourceEntry> resources = new ConcurrentHashMap<>();
    private final Map<String, PromptEntry> prompts = new ConcurrentHashMap<>();

    private Transport transport;
    private long requestIdCounter = 0;

    public DefaultMcpServer(String name, String version) {
        this.name = name;
        this.version = version;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public McpServer tool(String name, String description, ToolHandler handler) {
        tools.put(name, new ToolEntry(name, description, handler));
        return this;
    }

    @Override
    public McpServer resource(String uri, String name, String description, ResourceProvider provider) {
        resources.put(uri, new ResourceEntry(uri, name, description, provider));
        return this;
    }

    @Override
    public McpServer prompt(String name, String description, PromptHandler handler) {
        prompts.put(name, new PromptEntry(name, description, handler));
        return this;
    }

    @Override
    public void start(Transport transport) throws Exception {
        this.transport = transport;

        transport.onMessage(this::handleMessage);
        transport.onError(this::handleError);
        transport.onClose(this::handleClose);

        transport.start();
        log.info("MCP Server '{}' started", name);
    }

    @Override
    public void stop() throws Exception {
        if (transport != null) {
            transport.close();
        }
        log.info("MCP Server '{}' stopped", name);
    }

    @Override
    public List<Tool> listTools() {
        return tools.values().stream()
                .map(entry -> Tool.builder()
                        .name(entry.name)
                        .description(entry.description)
                        .build())
                .toList();
    }

    @Override
    public List<Resource> listResources() {
        return resources.values().stream()
                .map(entry -> Resource.builder()
                        .uri(entry.uri)
                        .name(entry.name)
                        .description(entry.description)
                        .build())
                .toList();
    }

    @Override
    public List<Prompt> listPrompts() {
        return prompts.values().stream()
                .map(entry -> Prompt.builder()
                        .name(entry.name)
                        .description(entry.description)
                        .build())
                .toList();
    }

    @Override
    public ToolCallResult callTool(String name, Map<String, Object> arguments) {
        ToolEntry entry = tools.get(name);
        if (entry == null) {
            throw McpException.toolNotFound(name);
        }
        return entry.handler.execute(arguments);
    }

    @Override
    public ResourceContent readResource(String uri) {
        ResourceEntry entry = resources.get(uri);
        if (entry == null) {
            throw McpException.resourceNotFound(uri);
        }
        return entry.provider.read(uri);
    }

    @Override
    public PromptResult getPrompt(String name, Map<String, String> arguments) {
        PromptEntry entry = prompts.get(name);
        if (entry == null) {
            throw McpException.promptNotFound(name);
        }
        return entry.handler.execute(arguments);
    }

    private void handleMessage(JsonRpcMessage message) {
        try {
            if (message.isRequest()) {
                handleRequest(message);
            } else if (message.isNotification()) {
                handleNotification(message);
            }
        } catch (Exception e) {
            log.error("Error handling message", e);
            sendErrorResponse(message.getId(), -32603, "Internal error: " + e.getMessage());
        }
    }

    private void handleRequest(JsonRpcMessage request) {
        String method = request.getMethod();
        Object id = request.getId();
        Object params = request.getParams();

        log.debug("Handling request: {}", method);

        switch (method) {
            case McpMethods.INITIALIZE -> handleInitialize(id, params);
            case McpMethods.TOOLS_LIST -> handleToolsList(id);
            case McpMethods.TOOLS_CALL -> handleToolsCall(id, params);
            case McpMethods.RESOURCES_LIST -> handleResourcesList(id);
            case McpMethods.RESOURCES_READ -> handleResourcesRead(id, params);
            case McpMethods.PROMPTS_LIST -> handlePromptsList(id);
            case McpMethods.PROMPTS_GET -> handlePromptsGet(id, params);
            case McpMethods.PING -> handlePing(id);
            default -> sendErrorResponse(id, -32601, "Method not found: " + method);
        }
    }

    private void handleNotification(JsonRpcMessage notification) {
        String method = notification.getMethod();
        log.debug("Handling notification: {}", method);

        switch (method) {
            case McpMethods.INITIALIZED -> log.info("Client initialized");
            case McpMethods.CANCELLED -> log.debug("Request cancelled");
            default -> log.warn("Unknown notification: {}", method);
        }
    }

    private void handleInitialize(Object id, Object params) {
        InitializeResult result = InitializeResult.builder()
                .protocolVersion(PROTOCOL_VERSION)
                .capabilities(InitializeResult.ServerCapabilities.builder()
                        .tools(InitializeResult.ToolsCapability.builder()
                                .listChanged(true)
                                .build())
                        .resources(InitializeResult.ResourcesCapability.builder()
                                .subscribe(false)
                                .listChanged(true)
                                .build())
                        .prompts(InitializeResult.PromptsCapability.builder()
                                .listChanged(true)
                                .build())
                        .build())
                .serverInfo(Implementation.builder()
                        .name(name)
                        .version(version)
                        .build())
                .build();

        sendResponse(id, result);
    }

    private void handleToolsList(Object id) {
        sendResponse(id, Map.of("tools", listTools()));
    }

    private void handleToolsCall(Object id, Object params) {
        try {
            ToolCallRequest request = objectMapper.convertValue(params, ToolCallRequest.class);
            ToolCallResult result = callTool(request.getName(), request.getArguments());
            sendResponse(id, result);
        } catch (McpException e) {
            sendErrorResponse(id, e.getCode(), e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(id, -32603, "Tool execution error: " + e.getMessage());
        }
    }

    private void handleResourcesList(Object id) {
        sendResponse(id, Map.of("resources", listResources()));
    }

    private void handleResourcesRead(Object id, Object params) {
        try {
            Map<String, Object> paramMap = (Map<String, Object>) params;
            String uri = (String) paramMap.get("uri");
            ResourceContent content = readResource(uri);
            sendResponse(id, Map.of("contents", List.of(content)));
        } catch (McpException e) {
            sendErrorResponse(id, e.getCode(), e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(id, -32603, "Resource read error: " + e.getMessage());
        }
    }

    private void handlePromptsList(Object id) {
        sendResponse(id, Map.of("prompts", listPrompts()));
    }

    private void handlePromptsGet(Object id, Object params) {
        try {
            Map<String, Object> paramMap = (Map<String, Object>) params;
            String name = (String) paramMap.get("name");
            Map<String, String> arguments = (Map<String, String>) paramMap.get("arguments");
            PromptResult result = getPrompt(name, arguments != null ? arguments : Map.of());
            sendResponse(id, result);
        } catch (McpException e) {
            sendErrorResponse(id, e.getCode(), e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(id, -32603, "Prompt get error: " + e.getMessage());
        }
    }

    private void handlePing(Object id) {
        sendResponse(id, Map.of());
    }

    private void sendResponse(Object id, Object result) {
        try {
            JsonRpcMessage response = JsonRpcMessage.successResponse(id, result);
            transport.send(response);
        } catch (Exception e) {
            log.error("Failed to send response", e);
        }
    }

    private void sendErrorResponse(Object id, int code, String message) {
        try {
            JsonRpcMessage response = JsonRpcMessage.errorResponse(id, code, message);
            transport.send(response);
        } catch (Exception e) {
            log.error("Failed to send error response", e);
        }
    }

    private void handleError(Throwable error) {
        log.error("Transport error", error);
    }

    private void handleClose() {
        log.info("Transport closed");
    }

    private record ToolEntry(String name, String description, ToolHandler handler) {}
    private record ResourceEntry(String uri, String name, String description, ResourceProvider provider) {}
    private record PromptEntry(String name, String description, PromptHandler handler) {}

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
        private String name = "mcp-server";
        private String version = "1.0.0";

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public DefaultMcpServer build() {
            return new DefaultMcpServer(name, version);
        }
    }
}
