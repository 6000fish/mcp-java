package com.mcp.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * McpException 单元测试
 */
class McpExceptionTest {

    @Test
    void testBasicException() {
        McpException ex = new McpException("Test error");

        assertEquals("Test error", ex.getMessage());
        assertEquals(-1, ex.getCode());
    }

    @Test
    void testExceptionWithCode() {
        McpException ex = new McpException(404, "Not found");

        assertEquals("Not found", ex.getMessage());
        assertEquals(404, ex.getCode());
    }

    @Test
    void testExceptionWithCause() {
        Exception cause = new RuntimeException("Root cause");
        McpException ex = new McpException("Test error", cause);

        assertEquals("Test error", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testToolNotFound() {
        McpException ex = McpException.toolNotFound("my_tool");

        assertEquals(-32601, ex.getCode());
        assertTrue(ex.getMessage().contains("my_tool"));
    }

    @Test
    void testResourceNotFound() {
        McpException ex = McpException.resourceNotFound("file:///test.txt");

        assertEquals(-32601, ex.getCode());
        assertTrue(ex.getMessage().contains("file:///test.txt"));
    }

    @Test
    void testPromptNotFound() {
        McpException ex = McpException.promptNotFound("my_prompt");

        assertEquals(-32601, ex.getCode());
        assertTrue(ex.getMessage().contains("my_prompt"));
    }

    @Test
    void testInvalidParams() {
        McpException ex = McpException.invalidParams("Missing required parameter");

        assertEquals(-32602, ex.getCode());
        assertTrue(ex.getMessage().contains("Missing required parameter"));
    }

    @Test
    void testInternalError() {
        McpException ex = McpException.internalError("Database connection failed");

        assertEquals(-32603, ex.getCode());
        assertTrue(ex.getMessage().contains("Database connection failed"));
    }

    @Test
    void testToolExecutionError() {
        McpException ex = McpException.toolExecutionError("query", "SQL syntax error");

        assertEquals(-32000, ex.getCode());
        assertTrue(ex.getMessage().contains("query"));
        assertTrue(ex.getMessage().contains("SQL syntax error"));
    }
}
