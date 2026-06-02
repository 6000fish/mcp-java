package com.mcp.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP 协议方法常量单元测试
 *
 * 测试目的：验证 MCP 协议定义的所有方法常量是否正确
 * 测试范围：初始化方法、工具方法、资源方法、提示方法、日志方法等
 */
class McpMethodsTest {

    /**
     * 测试初始化相关方法常量
     *
     * MCP 协议的初始化流程：
     * 1. 客户端发送 initialize 请求
     * 2. 服务端返回 capabilities
     * 3. 客户端发送 initialized 通知
     */
    @Test
    void testInitializeMethods() {
        // initialize 请求：客户端发送，用于初始化连接
        assertEquals("initialize", McpMethods.INITIALIZE);
        // initialized 通知：客户端发送，表示初始化完成
        assertEquals("notifications/initialized", McpMethods.INITIALIZED);
    }

    /**
     * 测试工具相关方法常量
     *
     * 工具是 MCP Server 提供的核心能力，客户端可以：
     * 1. 列出所有可用工具
     * 2. 调用指定工具
     */
    @Test
    void testToolMethods() {
        // tools/list：列出所有可用工具
        assertEquals("tools/list", McpMethods.TOOLS_LIST);
        // tools/call：调用指定工具
        assertEquals("tools/call", McpMethods.TOOLS_CALL);
    }

    /**
     * 测试资源相关方法常量
     *
     * 资源是 MCP Server 提供的数据源，支持：
     * 1. 列出所有资源
     * 2. 读取资源内容
     * 3. 订阅资源变更
     */
    @Test
    void testResourceMethods() {
        // resources/list：列出所有资源
        assertEquals("resources/list", McpMethods.RESOURCES_LIST);
        // resources/read：读取资源内容
        assertEquals("resources/read", McpMethods.RESOURCES_READ);
        // resources/subscribe：订阅资源变更通知
        assertEquals("resources/subscribe", McpMethods.RESOURCES_SUBSCRIBE);
        // resources/unsubscribe：取消订阅
        assertEquals("resources/unsubscribe", McpMethods.RESOURCES_UNSUBSCRIBE);
    }

    /**
     * 测试提示相关方法常量
     *
     * 提示是预定义的对话模板，支持：
     * 1. 列出所有提示
     * 2. 获取提示内容
     */
    @Test
    void testPromptMethods() {
        // prompts/list：列出所有提示
        assertEquals("prompts/list", McpMethods.PROMPTS_LIST);
        // prompts/get：获取提示内容
        assertEquals("prompts/get", McpMethods.PROMPTS_GET);
    }

    /**
     * 测试其他方法常量
     *
     * 包括：ping、日志、进度通知、取消等
     */
    @Test
    void testOtherMethods() {
        // ping：心跳检测
        assertEquals("ping", McpMethods.PING);
        // logging/setLevel：设置日志级别
        assertEquals("logging/setLevel", McpMethods.LOGGING_SET_LEVEL);
        // notifications/message：日志消息通知
        assertEquals("notifications/message", McpMethods.LOG_MESSAGE);
        // notifications/progress：进度通知
        assertEquals("notifications/progress", McpMethods.PROGRESS);
        // notifications/cancelled：取消通知
        assertEquals("notifications/cancelled", McpMethods.CANCELLED);
    }
}
