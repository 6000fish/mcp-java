package com.mcp.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP 异常单元测试
 *
 * 测试目的：验证 McpException 类的异常创建和错误码功能
 * 测试范围：基本异常、带错误码异常、带原因异常、各种预定义异常工厂方法
 */
class McpExceptionTest {

    /**
     * 测试创建基本异常
     *
     * 基本异常只包含错误消息，错误码默认为 -1
     *
     * 验证点：
     * 1. 错误消息正确
     * 2. 错误码为默认值 -1
     */
    @Test
    void testBasicException() {
        McpException ex = new McpException("Test error");

        assertEquals("Test error", ex.getMessage());
        assertEquals(-1, ex.getCode());
    }

    /**
     * 测试创建带错误码的异常
     *
     * 带错误码的异常用于返回 JSON-RPC 标准错误码
     *
     * 验证点：
     * 1. 错误消息正确
     * 2. 错误码正确设置
     */
    @Test
    void testExceptionWithCode() {
        McpException ex = new McpException(404, "Not found");

        assertEquals("Not found", ex.getMessage());
        assertEquals(404, ex.getCode());
    }

    /**
     * 测试创建带原因的异常
     *
     * 带原因的异常用于包装底层异常，保留原始错误信息
     *
     * 验证点：
     * 1. 错误消息正确
     * 2. 原因异常正确设置
     */
    @Test
    void testExceptionWithCause() {
        Exception cause = new RuntimeException("Root cause");
        McpException ex = new McpException("Test error", cause);

        assertEquals("Test error", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    /**
     * 测试工具未找到异常
     *
     * 当调用不存在的工具时抛出此异常
     * 错误码 -32601 表示方法未找到
     *
     * 验证点：
     * 1. 错误码为 -32601
     * 2. 错误消息包含工具名称
     */
    @Test
    void testToolNotFound() {
        McpException ex = McpException.toolNotFound("my_tool");

        assertEquals(-32601, ex.getCode());
        assertTrue(ex.getMessage().contains("my_tool"));
    }

    /**
     * 测试资源未找到异常
     *
     * 当访问不存在的资源时抛出此异常
     * 错误码 -32601 表示方法未找到
     *
     * 验证点：
     * 1. 错误码为 -32601
     * 2. 错误消息包含资源 URI
     */
    @Test
    void testResourceNotFound() {
        McpException ex = McpException.resourceNotFound("file:///test.txt");

        assertEquals(-32601, ex.getCode());
        assertTrue(ex.getMessage().contains("file:///test.txt"));
    }

    /**
     * 测试提示未找到异常
     *
     * 当获取不存在的提示时抛出此异常
     * 错误码 -32601 表示方法未找到
     *
     * 验证点：
     * 1. 错误码为 -32601
     * 2. 错误消息包含提示名称
     */
    @Test
    void testPromptNotFound() {
        McpException ex = McpException.promptNotFound("my_prompt");

        assertEquals(-32601, ex.getCode());
        assertTrue(ex.getMessage().contains("my_prompt"));
    }

    /**
     * 测试参数无效异常
     *
     * 当请求参数不符合要求时抛出此异常
     * 错误码 -32602 表示参数无效
     *
     * 验证点：
     * 1. 错误码为 -32602
     * 2. 错误消息包含参数错误描述
     */
    @Test
    void testInvalidParams() {
        McpException ex = McpException.invalidParams("Missing required parameter");

        assertEquals(-32602, ex.getCode());
        assertTrue(ex.getMessage().contains("Missing required parameter"));
    }

    /**
     * 测试内部错误异常
     *
     * 当服务端内部发生错误时抛出此异常
     * 错误码 -32603 表示内部错误
     *
     * 验证点：
     * 1. 错误码为 -32603
     * 2. 错误消息包含错误描述
     */
    @Test
    void testInternalError() {
        McpException ex = McpException.internalError("Database connection failed");

        assertEquals(-32603, ex.getCode());
        assertTrue(ex.getMessage().contains("Database connection failed"));
    }

    /**
     * 测试工具执行错误异常
     *
     * 当工具执行过程中发生错误时抛出此异常
     * 错误码 -32000 表示服务器错误
     *
     * 验证点：
     * 1. 错误码为 -32000
     * 2. 错误消息包含工具名称
     * 3. 错误消息包含错误描述
     */
    @Test
    void testToolExecutionError() {
        McpException ex = McpException.toolExecutionError("query", "SQL syntax error");

        assertEquals(-32000, ex.getCode());
        assertTrue(ex.getMessage().contains("query"));
        assertTrue(ex.getMessage().contains("SQL syntax error"));
    }
}
