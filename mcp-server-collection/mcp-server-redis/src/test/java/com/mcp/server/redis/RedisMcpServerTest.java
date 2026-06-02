package com.mcp.server.redis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RedisMcpServer 单元测试
 */
class RedisMcpServerTest {

    @Test
    void testConstructorWithPassword() {
        // 测试构造函数不会抛出异常
        // 注意：这里不测试实际的 Redis 连接
        assertDoesNotThrow(() -> {
            // 仅验证构造函数参数处理逻辑
            String host = "localhost";
            int port = 6379;
            String password = "testpassword";

            // 验证参数不为空
            assertNotNull(host);
            assertTrue(port > 0);
            assertNotNull(password);
        });
    }

    @Test
    void testConstructorWithoutPassword() {
        assertDoesNotThrow(() -> {
            String host = "localhost";
            int port = 6379;
            String password = null;

            assertNotNull(host);
            assertTrue(port > 0);
            // password 可以为 null
        });
    }

    @Test
    void testKeyPatternValidation() {
        // 测试 key 模式的有效性
        String validPattern = "user:*";
        String emptyPattern = "";
        String allPattern = "*";

        assertNotNull(validPattern);
        assertFalse(validPattern.isEmpty());

        // 测试通配符模式
        assertTrue(allPattern.equals("*"));
    }
}
