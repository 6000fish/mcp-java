package com.mcp.server;

import com.mcp.protocol.*;
import com.mcp.transport.Transport;

import java.util.List;
import java.util.Map;

/**
 * MCP 服务器接口
 * <p>
 * 定义了 MCP（Model Context Protocol）服务器的核心行为规范。
 * MCP 服务器是协议的服务端，负责注册和管理工具（Tools）、资源（Resources）、
 * Prompt 模板三大能力，并通过传输层（Transport）与客户端进行 JSON-RPC 通信。
 * </p>
 * <p>
 * 典型使用流程：
 * <pre>
 * McpServer server = DefaultMcpServer.builder()
 *         .name("my-server").version("1.0.0").build();
 * server.tool("greet", "打招呼", args -&gt; ...);
 * server.resource("file:///config", "配置", "应用配置", uri -&gt; ...);
 * server.prompt("review", "代码审查", args -&gt; ...);
 * server.start(new StdioTransport());
 * </pre>
 * </p>
 *
 * @see DefaultMcpServer
 * @see ToolHandler
 * @see ResourceProvider
 * @see PromptHandler
 */
public interface McpServer {

    /**
     * 注册工具到服务器
     * <p>
     * 工具是 MCP 协议中客户端可调用的可执行操作。注册后客户端可通过
     * {@code tools/list} 发现工具，通过 {@code tools/call} 调用工具。
     * </p>
     *
     * @param name        工具名称（在服务器内唯一标识）
     * @param description 工具描述（供客户端展示给用户或 AI 模型参考）
     * @param handler     工具执行处理器
     * @return 当前服务器实例，支持链式调用
     */
    McpServer tool(String name, String description, ToolHandler handler);

    /**
     * 注册带输入参数 Schema 的工具到服务器。
     *
     * @param name        工具名称（在服务器内唯一标识）
     * @param description 工具描述（供客户端展示给用户或 AI 模型参考）
     * @param inputSchema 工具输入参数的 JSON Schema
     * @param handler     工具执行处理器
     * @return 当前服务器实例，支持链式调用
     */
    default McpServer tool(String name, String description, Tool.InputSchema inputSchema, ToolHandler handler) {
        return tool(name, description, handler);
    }

    /**
     * 注册资源到服务器
     * <p>
     * 资源是 MCP 协议中客户端可读取的数据源。注册后客户端可通过
     * {@code resources/list} 发现资源，通过 {@code resources/read} 读取内容。
     * </p>
     *
     * @param uri         资源的唯一标识 URI（如 {@code "file:///path"}）
     * @param name        资源名称（供客户端展示）
     * @param description 资源描述（说明资源内容和用途）
     * @param provider    资源内容提供者
     * @return 当前服务器实例，支持链式调用
     */
    McpServer resource(String uri, String name, String description, ResourceProvider provider);

    /**
     * 注册 Prompt 模板到服务器
     * <p>
     * Prompt 模板是 MCP 协议中客户端可获取的结构化提示词。注册后客户端可通过
     * {@code prompts/list} 发现模板，通过 {@code prompts/get} 获取填充后的 Prompt。
     * </p>
     *
     * @param name        Prompt 名称（在服务器内唯一标识）
     * @param description Prompt 描述（说明模板用途）
     * @param handler     Prompt 处理器
     * @return 当前服务器实例，支持链式调用
     */
    McpServer prompt(String name, String description, PromptHandler handler);

    /**
     * 启动 MCP 服务器
     * <p>
     * 绑定传输层并开始监听客户端请求。服务器会注册消息、错误和关闭的回调处理。
     * </p>
     *
     * @param transport 传输层实现（如 Stdio、SSE）
     * @throws Exception 启动过程中可能抛出的异常
     */
    void start(Transport transport) throws Exception;

    /**
     * 停止 MCP 服务器
     * <p>
     * 关闭传输层连接，释放相关资源。
     * </p>
     *
     * @throws Exception 停止过程中可能抛出的异常
     */
    void stop() throws Exception;

    /**
     * 获取已注册的所有工具列表
     * <p>
     * 返回工具的元数据（名称和描述），不包含处理器本身。
     * 通常由服务器内部在处理 {@code tools/list} 请求时调用。
     * </p>
     *
     * @return 工具元数据列表
     */
    List<Tool> listTools();

    /**
     * 获取已注册的所有资源列表
     * <p>
     * 返回资源的元数据（URI、名称和描述），不包含实际内容。
     * 通常由服务器内部在处理 {@code resources/list} 请求时调用。
     * </p>
     *
     * @return 资源元数据列表
     */
    List<Resource> listResources();

    /**
     * 获取已注册的所有 Prompt 模板列表
     * <p>
     * 返回 Prompt 的元数据（名称和描述），不包含模板内容。
     * 通常由服务器内部在处理 {@code prompts/list} 请求时调用。
     * </p>
     *
     * @return Prompt 元数据列表
     */
    List<Prompt> listPrompts();

    /**
     * 调用指定名称的工具
     * <p>
     * 根据工具名称查找已注册的工具处理器并执行。若工具不存在，
     * 抛出 {@link com.mcp.exception.McpException}。
     * </p>
     *
     * @param name      要调用的工具名称
     * @param arguments 工具执行参数
     * @return 工具执行结果
     * @throws com.mcp.exception.McpException 工具不存在或执行失败时抛出
     */
    ToolCallResult callTool(String name, Map<String, Object> arguments);

    /**
     * 读取指定 URI 的资源内容
     * <p>
     * 根据 URI 查找已注册的资源提供者并读取内容。若资源不存在，
     * 抛出 {@link com.mcp.exception.McpException}。
     * </p>
     *
     * @param uri 资源的唯一标识 URI
     * @return 资源内容
     * @throws com.mcp.exception.McpException 资源不存在或读取失败时抛出
     */
    ResourceContent readResource(String uri);

    /**
     * 获取指定名称的 Prompt 内容
     * <p>
     * 根据名称查找已注册的 Prompt 处理器并执行，返回填充参数后的 Prompt 结果。
     * 若 Prompt 不存在，抛出 {@link com.mcp.exception.McpException}。
     * </p>
     *
     * @param name      Prompt 名称
     * @param arguments Prompt 模板参数（键为参数名，值为参数值）
     * @return Prompt 执行结果
     * @throws com.mcp.exception.McpException Prompt 不存在或执行失败时抛出
     */
    PromptResult getPrompt(String name, Map<String, String> arguments);
}
