package com.mcp.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JSON-RPC 消息单元测试
 *
 * 测试目的：验证 JsonRpcMessage 类的消息创建、序列化和类型判断功能
 * 测试范围：请求消息、响应消息、通知消息、错误消息的创建和属性验证
 */
class JsonRpcMessageTest {

    /**
     * 测试创建请求消息
     *
     * 验证点：
     * 1. jsonrpc 版本号默认为 "2.0"
     * 2. id、method、params 属性正确设置
     * 3. result 和 error 属性为空
     * 4. isRequest() 返回 true
     * 5. isNotification() 和 isResponse() 返回 false
     */
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

    /**
     * 测试创建成功响应消息
     *
     * 验证点：
     * 1. jsonrpc 版本号默认为 "2.0"
     * 2. id 和 result 属性正确设置
     * 3. method 属性为空（响应消息没有 method）
     * 4. error 属性为空
     * 5. isResponse() 返回 true
     * 6. isRequest() 和 isNotification() 返回 false
     */
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

    /**
     * 测试创建错误响应消息
     *
     * 验证点：
     * 1. id 属性正确设置
     * 2. error 属性包含错误码和错误消息
     * 3. result 属性为空
     */
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

    /**
     * 测试创建通知消息
     *
     * 通知消息是没有 id 的请求消息，用于单向通知，不需要响应
     *
     * 验证点：
     * 1. id 属性为空
     * 2. method 和 params 属性正确设置
     * 3. isNotification() 返回 true
     * 4. isRequest() 和 isResponse() 返回 false
     */
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
