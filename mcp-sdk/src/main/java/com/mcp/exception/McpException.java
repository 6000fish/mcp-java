package com.mcp.exception;

/**
 * MCP 协议异常基类。
 * <p>
 * 表示在 MCP 通信过程中发生的各类错误，包含 JSON-RPC 标准错误码和自定义错误码。
 * 错误码遵循 JSON-RPC 2.0 规范：
 * </p>
 * <ul>
 *   <li>{@code -32601} - 方法/工具/资源未找到</li>
 *   <li>{@code -32602} - 无效的参数</li>
 *   <li>{@code -32603} - 内部错误</li>
 *   <li>{@code -32000} - 工具执行错误（自定义）</li>
 * </ul>
 * <p>
 * 提供了多个静态工厂方法用于快速创建常见类型的异常。
 * </p>
 */
public class McpException extends RuntimeException {

    /** JSON-RPC 错误码 */
    private final int code;

    /**
     * 使用错误消息创建异常，错误码默认为 {@code -1}。
     *
     * @param message 错误描述信息
     */
    public McpException(String message) {
        super(message);
        this.code = -1;
    }

    /**
     * 使用指定的错误码和错误消息创建异常。
     *
     * @param code    JSON-RPC 错误码
     * @param message 错误描述信息
     */
    public McpException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 使用错误消息和原始原因创建异常，错误码默认为 {@code -1}。
     *
     * @param message 错误描述信息
     * @param cause   导致此异常的原始异常
     */
    public McpException(String message, Throwable cause) {
        super(message, cause);
        this.code = -1;
    }

    /**
     * 使用指定的错误码、错误消息和原始原因创建异常。
     *
     * @param code    JSON-RPC 错误码
     * @param message 错误描述信息
     * @param cause   导致此异常的原始异常
     */
    public McpException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * 获取 JSON-RPC 错误码。
     *
     * @return 错误码整数值
     */
    public int getCode() {
        return code;
    }

    /**
     * 创建"工具未找到"异常（错误码 {@code -32601}）。
     *
     * @param name 未找到的工具名称
     * @return 包含错误信息的 {@link McpException} 实例
     */
    public static McpException toolNotFound(String name) {
        return new McpException(-32601, "Tool not found: " + name);
    }

    /**
     * 创建"资源未找到"异常（错误码 {@code -32601}）。
     *
     * @param uri 未找到的资源 URI
     * @return 包含错误信息的 {@link McpException} 实例
     */
    public static McpException resourceNotFound(String uri) {
        return new McpException(-32601, "Resource not found: " + uri);
    }

    /**
     * 创建"Prompt 未找到"异常（错误码 {@code -32601}）。
     *
     * @param name 未找到的 Prompt 名称
     * @return 包含错误信息的 {@link McpException} 实例
     */
    public static McpException promptNotFound(String name) {
        return new McpException(-32601, "Prompt not found: " + name);
    }

    /**
     * 创建"参数无效"异常（错误码 {@code -32602}）。
     *
     * @param message 参数错误的详细描述
     * @return 包含错误信息的 {@link McpException} 实例
     */
    public static McpException invalidParams(String message) {
        return new McpException(-32602, "Invalid params: " + message);
    }

    /**
     * 创建"内部错误"异常（错误码 {@code -32603}）。
     *
     * @param message 内部错误的详细描述
     * @return 包含错误信息的 {@link McpException} 实例
     */
    public static McpException internalError(String message) {
        return new McpException(-32603, "Internal error: " + message);
    }

    /**
     * 创建"工具执行错误"异常（错误码 {@code -32000}，自定义错误码）。
     *
     * @param toolName 执行失败的工具名称
     * @param message  执行错误的详细描述
     * @return 包含错误信息的 {@link McpException} 实例
     */
    public static McpException toolExecutionError(String toolName, String message) {
        return new McpException(-32000, "Tool execution error [" + toolName + "]: " + message);
    }
}
