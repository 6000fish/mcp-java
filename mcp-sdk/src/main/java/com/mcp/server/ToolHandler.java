package com.mcp.server;

import com.mcp.protocol.ToolCallResult;

import java.util.Map;

/**
 * 工具处理器接口（函数式接口）
 * <p>
 * MCP 服务器端的核心扩展点之一。实现此接口可注册自定义工具，
 * 当客户端通过 MCP 协议发起 {@code tools/call} 请求时，
 * 服务器会调用对应 ToolHandler 的 {@link #execute} 方法来执行工具逻辑。
 * </p>
 * <p>
 * 作为 {@code @FunctionalInterface}，支持 Lambda 表达式简化注册：
 * <pre>
 * server.tool("greet", "打招呼工具", args -&gt; new ToolCallResult(...));
 * </pre>
 * </p>
 *
 * @see McpServer#tool(String, String, ToolHandler)
 */
@FunctionalInterface
public interface ToolHandler {

    /**
     * 执行工具调用
     * <p>
     * 接收客户端传入的参数 Map，执行具体的工具逻辑并返回结果。
     * 参数的 key 和 value 由工具定义决定，通常以 JSON Schema 描述。
     * </p>
     *
     * @param arguments 工具参数，键为参数名，值为参数值（类型由工具定义决定）
     * @return 工具执行结果，包含执行状态和返回内容
     */
    ToolCallResult execute(Map<String, Object> arguments);
}
