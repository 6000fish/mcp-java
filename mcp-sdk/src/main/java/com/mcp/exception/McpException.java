package com.mcp.exception;

/**
 * MCP 异常基类
 */
public class McpException extends RuntimeException {

    private final int code;

    public McpException(String message) {
        super(message);
        this.code = -1;
    }

    public McpException(int code, String message) {
        super(message);
        this.code = code;
    }

    public McpException(String message, Throwable cause) {
        super(message, cause);
        this.code = -1;
    }

    public McpException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 工具未找到
     */
    public static McpException toolNotFound(String name) {
        return new McpException(-32601, "Tool not found: " + name);
    }

    /**
     * 资源未找到
     */
    public static McpException resourceNotFound(String uri) {
        return new McpException(-32601, "Resource not found: " + uri);
    }

    /**
     * Prompt 未找到
     */
    public static McpException promptNotFound(String name) {
        return new McpException(-32601, "Prompt not found: " + name);
    }

    /**
     * 参数错误
     */
    public static McpException invalidParams(String message) {
        return new McpException(-32602, "Invalid params: " + message);
    }

    /**
     * 内部错误
     */
    public static McpException internalError(String message) {
        return new McpException(-32603, "Internal error: " + message);
    }

    /**
     * 工具执行错误
     */
    public static McpException toolExecutionError(String toolName, String message) {
        return new McpException(-32000, "Tool execution error [" + toolName + "]: " + message);
    }
}
