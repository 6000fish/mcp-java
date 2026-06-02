package com.mcp.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * McpMethods 单元测试
 */
class McpMethodsTest {

    @Test
    void testInitializeMethods() {
        assertEquals("initialize", McpMethods.INITIALIZE);
        assertEquals("notifications/initialized", McpMethods.INITIALIZED);
    }

    @Test
    void testToolMethods() {
        assertEquals("tools/list", McpMethods.TOOLS_LIST);
        assertEquals("tools/call", McpMethods.TOOLS_CALL);
    }

    @Test
    void testResourceMethods() {
        assertEquals("resources/list", McpMethods.RESOURCES_LIST);
        assertEquals("resources/read", McpMethods.RESOURCES_READ);
        assertEquals("resources/subscribe", McpMethods.RESOURCES_SUBSCRIBE);
        assertEquals("resources/unsubscribe", McpMethods.RESOURCES_UNSUBSCRIBE);
    }

    @Test
    void testPromptMethods() {
        assertEquals("prompts/list", McpMethods.PROMPTS_LIST);
        assertEquals("prompts/get", McpMethods.PROMPTS_GET);
    }

    @Test
    void testOtherMethods() {
        assertEquals("ping", McpMethods.PING);
        assertEquals("logging/setLevel", McpMethods.LOGGING_SET_LEVEL);
        assertEquals("notifications/message", McpMethods.LOG_MESSAGE);
    }
}
