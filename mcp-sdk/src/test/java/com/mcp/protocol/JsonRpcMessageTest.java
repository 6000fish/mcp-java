package com.mcp.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JsonRpcMessage 单元测试
 */
class JsonRpcMessageTest {

    @Test
    void testRequest() {
        JsonRpcMessage request = JsonRpcMessage.request(1, "test/method", null);

        assertEquals("2.0", request.getJsonrpc());
        assertEquals(1, request.getId());
        assertEquals("test/method", request.getMethod());
        assertNull(request.getResult());
        assertNull(request.getError());

        assertTrue(request.isRequest());
        assertFalse(request.isNotification());
        assertFalse(request.isResponse());
    }

    @Test
    void testSuccessResponse() {
        JsonRpcMessage response = JsonRpcMessage.successResponse(1, "result");

        assertEquals("2.0", response.getJsonrpc());
        assertEquals(1, response.getId());
        assertNull(response.getMethod());
        assertEquals("result", response.getResult());
        assertNull(response.getError());

        assertFalse(response.isRequest());
        assertFalse(response.isNotification());
        assertTrue(response.isResponse());
    }

    @Test
    void testErrorResponse() {
        JsonRpcMessage response = JsonRpcMessage.errorResponse(1, -32600, "Invalid Request");

        assertEquals("2.0", response.getJsonrpc());
        assertEquals(1, response.getId());
        assertNull(response.getMethod());
        assertNull(response.getResult());
        assertNotNull(response.getError());
        assertEquals(-32600, response.getError().getCode());
        assertEquals("Invalid Request", response.getError().getMessage());
    }

    @Test
    void testNotification() {
        JsonRpcMessage notification = JsonRpcMessage.notification("test/event", null);

        assertEquals("2.0", notification.getJsonrpc());
        assertNull(notification.getId());
        assertEquals("test/event", notification.getMethod());
        assertNull(notification.getResult());
        assertNull(notification.getError());

        assertFalse(notification.isRequest());
        assertTrue(notification.isNotification());
        assertFalse(notification.isResponse());
    }
}
