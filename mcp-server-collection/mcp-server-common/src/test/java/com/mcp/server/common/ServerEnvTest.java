package com.mcp.server.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerEnvTest {

    @Test
    void getReturnsDefaultWhenEnvMissing() {
        assertEquals("fallback", ServerEnv.get("MCP_TEST_MISSING_ENV", "fallback"));
    }

    @Test
    void getIntReturnsDefaultWhenEnvMissing() {
        assertEquals(3306, ServerEnv.getInt("MCP_TEST_MISSING_PORT", 3306));
    }

    @Test
    void getIntThrowsClearErrorForInvalidValue() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ServerEnv.getInt("PATH", 3306));
        assertTrue(exception.getMessage().contains("PATH"));
    }
}
