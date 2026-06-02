package com.mcp.client;

import com.mcp.protocol.*;
import com.mcp.transport.Transport;

import java.util.List;
import java.util.Map;

/**
 * MCP 客户端接口
 * <p>
 * 定义了 MCP（Model Context Protocol）客户端的核心行为规范。
 * MCP 客户端是协议的消费端，负责与 MCP 服务器建立连接，并通过
 * JSON-RPC 协议调用服务器提供的工具、资源和 Prompt 能力。
 * </p>
 * <p>
 * 典型使用流程：
 * <pre>
 * McpClient client = DefaultMcpClient.builder()
 *         .name("my-client").version("1.0.0").build();
 * client.connect(new StdioTransport());
 * client.initialize(new InitializeRequest(...));
 * List&lt;Tool&gt; tools = client.listTools();
 * ToolCallResult result = client.callTool("greet", Map.of("name", "Alice"));
 * client.disconnect();
 * </pre>
 * </p>
 *
 * @see DefaultMcpClient
 */
public interface McpClient {

    /**
     * 连接到 MCP 服务器
     * <p>
     * 绑定传输层并建立与服务器的通信通道。连接成功后需调用
     * {@link #initialize(InitializeRequest)} 完成协议握手。
     * </p>
     *
     * @param transport 传输层实现（如 Stdio、SSE、WebSocket 等）
     * @throws Exception 连接过程中可能抛出的异常
     */
    void connect(Transport transport) throws Exception;

    /**
     * 断开与 MCP 服务器的连接
     * <p>
     * 关闭传输层连接，清理所有等待中的请求，释放相关资源。
     * </p>
     *
     * @throws Exception 断开连接过程中可能抛出的异常
     */
    void disconnect() throws Exception;

    /**
     * 初始化 MCP 连接（协议握手）
     * <p>
     * 向服务器发送初始化请求，协商协议版本和能力集。
     * 必须在 {@link #connect(Transport)} 之后、执行其他操作之前调用。
     * </p>
     *
     * @param request 初始化请求，包含客户端信息和声明的能力
     * @return 初始化结果，包含协议版本和服务器能力
     * @throws Exception 初始化失败时抛出
     */
    InitializeResult initialize(InitializeRequest request) throws Exception;

    /**
     * 列出服务器提供的所有可用工具
     * <p>
     * 发送 {@code tools/list} 请求获取服务器注册的工具元数据列表。
     * </p>
     *
     * @return 工具元数据列表（名称和描述）
     * @throws Exception 请求失败时抛出
     */
    List<Tool> listTools() throws Exception;

    /**
     * 调用服务器上的指定工具
     * <p>
     * 发送 {@code tools/call} 请求，由服务器执行对应工具并返回结果。
     * </p>
     *
     * @param name      要调用的工具名称
     * @param arguments 工具执行参数（键为参数名，值为参数值）
     * @return 工具执行结果
     * @throws Exception 工具不存在或执行失败时抛出
     */
    ToolCallResult callTool(String name, Map<String, Object> arguments) throws Exception;

    /**
     * 列出服务器提供的所有可用资源
     * <p>
     * 发送 {@code resources/list} 请求获取服务器注册的资源元数据列表。
     * </p>
     *
     * @return 资源元数据列表（URI、名称和描述）
     * @throws Exception 请求失败时抛出
     */
    List<Resource> listResources() throws Exception;

    /**
     * 读取服务器上指定 URI 的资源内容
     * <p>
     * 发送 {@code resources/read} 请求，由服务器读取并返回资源内容。
     * </p>
     *
     * @param uri 资源的唯一标识 URI
     * @return 资源内容列表
     * @throws Exception 资源不存在或读取失败时抛出
     */
    List<ResourceContent> readResource(String uri) throws Exception;

    /**
     * 列出服务器提供的所有可用 Prompt 模板
     * <p>
     * 发送 {@code prompts/list} 请求获取服务器注册的 Prompt 元数据列表。
     * </p>
     *
     * @return Prompt 元数据列表（名称和描述）
     * @throws Exception 请求失败时抛出
     */
    List<Prompt> listPrompts() throws Exception;

    /**
     * 获取服务器上指定名称的 Prompt 内容
     * <p>
     * 发送 {@code prompts/get} 请求，由服务器填充模板参数并返回 Prompt 结果。
     * </p>
     *
     * @param name      Prompt 名称
     * @param arguments Prompt 模板参数（键为参数名，值为参数值）
     * @return Prompt 执行结果
     * @throws Exception Prompt 不存在或获取失败时抛出
     */
    PromptResult getPrompt(String name, Map<String, String> arguments) throws Exception;

    /**
     * 判断客户端是否已连接到服务器
     *
     * @return {@code true} 表示已连接，{@code false} 表示未连接
     */
    boolean isConnected();
}
