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
 * MCP 服务器默认实现
 * <p>
 * 基于 JSON-RPC 2.0 协议的 MCP 服务器完整实现，提供工具（Tools）、
 * 资源（Resources）和 Prompt 模板三大能力的注册、管理和请求处理。
 * </p>
 * <p>
 * 核心职责：
 * <ul>
 *   <li>管理已注册的工具、资源和 Prompt 处理器</li>
 *   <li>解析客户端 JSON-RPC 请求并路由到对应的处理器</li>
 *   <li>构造并发送 JSON-RPC 响应（成功或错误）</li>
 *   <li>处理传输层的错误和关闭事件</li>
 * </ul>
 * </p>
 * <p>
 * 支持通过 Builder 模式创建实例：
 * <pre>
 * DefaultMcpServer server = DefaultMcpServer.builder()
 *         .name("my-server").version("1.0.0").build();
 * </pre>
 * </p>
 *
 * @see McpServer
 */
public class DefaultMcpServer implements McpServer {

    /** 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(DefaultMcpServer.class);

    /** MCP 协议版本号，遵循 "YYYY-MM-DD" 格式 */
    private static final String PROTOCOL_VERSION = "2024-11-05";

    /** 服务器名称，用于标识和初始化握手时返回给客户端 */
    private final String name;

    /** 服务器版本号，用于标识和初始化握手时返回给客户端 */
    private final String version;

    /** JSON 序列化/反序列化器，用于处理 JSON-RPC 消息中的参数转换 */
    private final ObjectMapper objectMapper;

    /** 已注册的工具映射表，key 为工具名称，value 为工具条目（包含名称、描述和处理器） */
    private final Map<String, ToolEntry> tools = new ConcurrentHashMap<>();

    /** 已注册的资源映射表，key 为资源 URI，value 为资源条目（包含 URI、名称、描述和提供者） */
    private final Map<String, ResourceEntry> resources = new ConcurrentHashMap<>();

    /** 已注册的 Prompt 模板映射表，key 为 Prompt 名称，value 为 Prompt 条目（包含名称、描述和处理器） */
    private final Map<String, PromptEntry> prompts = new ConcurrentHashMap<>();

    /** 传输层实例，负责底层通信（如 Stdio、SSE、WebSocket 等） */
    private Transport transport;

    /** 请求 ID 计数器，用于生成唯一的 JSON-RPC 请求标识 */
    private long requestIdCounter = 0;

    /**
     * 创建 DefaultMcpServer 实例
     *
     * @param name    服务器名称
     * @param version 服务器版本号
     */
    public DefaultMcpServer(String name, String version) {
        this.name = name;
        this.version = version;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public McpServer tool(String name, String description, ToolHandler handler) {
        return tool(name, description, emptyInputSchema(), handler);
    }

    @Override
    public McpServer tool(String name, String description, Tool.InputSchema inputSchema, ToolHandler handler) {
        tools.put(name, new ToolEntry(name, description, inputSchema, handler));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public McpServer resource(String uri, String name, String description, ResourceProvider provider) {
        resources.put(uri, new ResourceEntry(uri, name, description, provider));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public McpServer prompt(String name, String description, PromptHandler handler) {
        prompts.put(name, new PromptEntry(name, description, handler));
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * 注册消息处理、错误处理和连接关闭的回调，然后启动传输层。
     * </p>
     */
    @Override
    public void start(Transport transport) throws Exception {
        this.transport = transport;

        transport.onMessage(this::handleMessage);
        transport.onError(this::handleError);
        transport.onClose(this::handleClose);

        transport.start();
        log.info("MCP Server '{}' started", name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() throws Exception {
        if (transport != null) {
            transport.close();
        }
        log.info("MCP Server '{}' stopped", name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Tool> listTools() {
        return tools.values().stream()
                .map(entry -> Tool.builder()
                        .name(entry.name)
                        .description(entry.description)
                        .inputSchema(entry.inputSchema)
                        .build())
                .toList();
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Prompt> listPrompts() {
        return prompts.values().stream()
                .map(entry -> Prompt.builder()
                        .name(entry.name)
                        .description(entry.description)
                        .build())
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ToolCallResult callTool(String name, Map<String, Object> arguments) {
        ToolEntry entry = tools.get(name);
        if (entry == null) {
            throw McpException.toolNotFound(name);
        }
        return entry.handler.execute(arguments);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceContent readResource(String uri) {
        ResourceEntry entry = resources.get(uri);
        if (entry == null) {
            // 尝试 URI 模板匹配（如 file:///{path} 匹配 file:///etc/hosts）
            for (ResourceEntry re : resources.values()) {
                if (matchesUriTemplate(re.uri, uri)) {
                    entry = re;
                    break;
                }
            }
        }
        if (entry == null) {
            throw McpException.resourceNotFound(uri);
        }
        return entry.provider.read(uri);
    }

    /**
     * 判断实际 URI 是否匹配模板 URI。
     * 将模板中的 {param} 替换为 [^/]+ 正则，然后进行匹配。
     */
    private boolean matchesUriTemplate(String template, String actual) {
        String regex = template.replaceAll("\\{[^}]+}", "(.+)");
        return actual.matches(regex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PromptResult getPrompt(String name, Map<String, String> arguments) {
        PromptEntry entry = prompts.get(name);
        if (entry == null) {
            throw McpException.promptNotFound(name);
        }
        return entry.handler.execute(arguments);
    }

    /**
     * 处理从传输层接收到的 JSON-RPC 消息
     * <p>
     * 根据消息类型（请求或通知）分发到对应的处理方法。
     * 处理过程中发生的异常会被捕获并转换为错误响应发送给客户端。
     * </p>
     *
     * @param message 接收到的 JSON-RPC 消息
     */
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

    /**
     * 处理 JSON-RPC 请求消息
     * <p>
     * 根据请求方法名路由到对应的处理逻辑，包括：
     * initialize、tools/list、tools/call、resources/list、resources/read、
     * prompts/list、prompts/get、ping 等。
     * 未知方法将返回 -32601 错误码。
     * </p>
     *
     * @param request JSON-RPC 请求消息
     */
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

    /**
     * 处理 JSON-RPC 通知消息（无需响应）
     * <p>
     * 处理客户端发送的通知，如 {@code initialized}（初始化完成通知）
     * 和 {@code notifications/cancelled}（请求取消通知）。
     * </p>
     *
     * @param notification JSON-RPC 通知消息
     */
    private void handleNotification(JsonRpcMessage notification) {
        String method = notification.getMethod();
        log.debug("Handling notification: {}", method);

        switch (method) {
            case McpMethods.INITIALIZED -> log.info("Client initialized");
            case McpMethods.CANCELLED -> log.debug("Request cancelled");
            default -> log.warn("Unknown notification: {}", method);
        }
    }

    /**
     * 处理初始化请求（{@code initialize}）
     * <p>
     * 构造初始化响应，包含协议版本、服务器能力和服务器信息。
     * </p>
     *
     * @param id     请求 ID，用于匹配响应
     * @param params 请求参数（当前未使用，预留扩展）
     */
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

    /**
     * 处理工具列表请求（{@code tools/list}）
     *
     * @param id 请求 ID
     */
    private void handleToolsList(Object id) {
        sendResponse(id, Map.of("tools", listTools()));
    }

    /**
     * 处理工具调用请求（{@code tools/call}）
     * <p>
     * 解析请求参数，查找并执行对应的工具处理器。
     * 工具不存在时返回 MCP 错误码，执行异常时返回 -32603 内部错误。
     * </p>
     *
     * @param id     请求 ID
     * @param params 请求参数，包含工具名称和参数
     */
    private void handleToolsCall(Object id, Object params) {
        try {
            ToolCallRequest request = objectMapper.convertValue(paramsAsMap(params), ToolCallRequest.class);
            ToolCallResult result = callTool(request.getName(), request.getArguments() != null ? request.getArguments() : Map.of());
            sendResponse(id, result);
        } catch (McpException e) {
            sendErrorResponse(id, e.getCode(), e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(id, -32603, "Tool execution error: " + e.getMessage());
        }
    }

    /**
     * 处理资源列表请求（{@code resources/list}）
     *
     * @param id 请求 ID
     */
    private void handleResourcesList(Object id) {
        sendResponse(id, Map.of("resources", listResources()));
    }

    /**
     * 处理资源读取请求（{@code resources/read}）
     * <p>
     * 解析请求参数中的 URI，查找并调用对应的资源提供者。
     * 资源不存在时返回 MCP 错误码，读取异常时返回 -32603 内部错误。
     * </p>
     *
     * @param id     请求 ID
     * @param params 请求参数，包含资源 URI
     */
    private void handleResourcesRead(Object id, Object params) {
        try {
            Map<String, Object> paramMap = paramsAsMap(params);
            String uri = (String) paramMap.get("uri");
            ResourceContent content = readResource(uri);
            sendResponse(id, Map.of("contents", List.of(content)));
        } catch (McpException e) {
            sendErrorResponse(id, e.getCode(), e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(id, -32603, "Resource read error: " + e.getMessage());
        }
    }

    /**
     * 处理 Prompt 列表请求（{@code prompts/list}）
     *
     * @param id 请求 ID
     */
    private void handlePromptsList(Object id) {
        sendResponse(id, Map.of("prompts", listPrompts()));
    }

    /**
     * 处理 Prompt 获取请求（{@code prompts/get}）
     * <p>
     * 解析请求参数中的名称和模板参数，查找并执行对应的 Prompt 处理器。
     * Prompt 不存在时返回 MCP 错误码，执行异常时返回 -32603 内部错误。
     * </p>
     *
     * @param id     请求 ID
     * @param params 请求参数，包含 Prompt 名称和模板参数
     */
    private void handlePromptsGet(Object id, Object params) {
        try {
            Map<String, Object> paramMap = paramsAsMap(params);
            String name = (String) paramMap.get("name");
            PromptResult result = getPrompt(name, stringArguments(paramMap.get("arguments")));
            sendResponse(id, result);
        } catch (McpException e) {
            sendErrorResponse(id, e.getCode(), e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(id, -32603, "Prompt get error: " + e.getMessage());
        }
    }

    /**
     * 处理 Ping 请求（{@code ping}）
     * <p>
     * 返回空对象作为响应，用于检测服务器是否存活。
     * </p>
     *
     * @param id 请求 ID
     */
    private void handlePing(Object id) {
        sendResponse(id, Map.of());
    }

    /**
     * 发送成功的 JSON-RPC 响应
     *
     * @param id     请求 ID，用于客户端匹配响应
     * @param result 响应结果数据
     */
    private void sendResponse(Object id, Object result) {
        try {
            JsonRpcMessage response = JsonRpcMessage.successResponse(id, result);
            transport.send(response);
        } catch (Exception e) {
            log.error("Failed to send response", e);
        }
    }

    /**
     * 发送错误的 JSON-RPC 响应
     *
     * @param id      请求 ID
     * @param code    错误码（遵循 JSON-RPC 规范，如 -32601 方法未找到、-32603 内部错误）
     * @param message 错误描述信息
     */
    private void sendErrorResponse(Object id, int code, String message) {
        try {
            JsonRpcMessage response = JsonRpcMessage.errorResponse(id, code, message);
            transport.send(response);
        } catch (Exception e) {
            log.error("Failed to send error response", e);
        }
    }

    /**
     * 传输层错误回调
     *
     * @param error 错误信息
     */
    private void handleError(Throwable error) {
        log.error("Transport error", error);
    }

    /**
     * 传输层关闭回调
     */
    private void handleClose() {
        log.info("Transport closed");
    }

    private static Map<String, Object> paramsAsMap(Object params) {
        if (!(params instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }

        Map<String, Object> result = new HashMap<>();
        rawMap.forEach((key, value) -> {
            if (key instanceof String stringKey) {
                result.put(stringKey, value);
            }
        });
        return result;
    }

    private static Map<String, String> stringArguments(Object arguments) {
        if (!(arguments instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }

        Map<String, String> result = new HashMap<>();
        rawMap.forEach((key, value) -> {
            if (key instanceof String stringKey && value != null) {
                result.put(stringKey, String.valueOf(value));
            }
        });
        return result;
    }

    private static Tool.InputSchema emptyInputSchema() {
        return Tool.InputSchema.builder()
                .type("object")
                .properties(Map.of())
                .required(List.of())
                .build();
    }

    /**
     * 工具条目，封装工具的元数据和处理器
     */
    private record ToolEntry(String name, String description, Tool.InputSchema inputSchema, ToolHandler handler) {}

    /**
     * 资源条目，封装资源的元数据和提供者
     */
    private record ResourceEntry(String uri, String name, String description, ResourceProvider provider) {}

    /**
     * Prompt 条目，封装 Prompt 的元数据和处理器
     */
    private record PromptEntry(String name, String description, PromptHandler handler) {}

    /**
     * 创建 DefaultMcpServer 的 Builder 实例
     *
     * @return 新的 Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * DefaultMcpServer 构建器
     * <p>
     * 支持链式调用设置服务器名称和版本号，最终通过 {@link #build()} 创建实例。
     * </p>
     */
    public static class Builder {
        /** 服务器名称，默认为 "mcp-server" */
        private String name = "mcp-server";
        /** 服务器版本号，默认为 "1.0.0" */
        private String version = "1.0.0";

        /**
         * 设置服务器名称
         *
         * @param name 服务器名称
         * @return 当前 Builder 实例
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 设置服务器版本号
         *
         * @param version 版本号
         * @return 当前 Builder 实例
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * 构建 DefaultMcpServer 实例
         *
         * @return 配置好的 DefaultMcpServer 实例
         */
        public DefaultMcpServer build() {
            return new DefaultMcpServer(name, version);
        }
    }
}
