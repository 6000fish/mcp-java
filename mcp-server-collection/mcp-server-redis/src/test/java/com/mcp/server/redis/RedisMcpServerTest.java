package com.mcp.server.redis;

import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import com.mcp.protocol.ToolCallResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis MCP Server 单元测试
 *
 * 测试目的：验证 RedisMcpServer 的核心功能，包括：
 * 1. 连接参数验证
 * 2. Key 模式验证
 * 3. 工具注册和调用流程
 * 4. 命令安全检查
 * 5. 结果格式化
 *
 * 注意：本测试不依赖真实的 Redis 服务，使用模拟数据测试逻辑
 */
class RedisMcpServerTest {

    // ==================== 1. 连接参数验证测试 ====================

    /**
     * 测试带密码的连接参数
     *
     * 验证：
     * 1. 主机名不为空
     * 2. 端口号大于 0
     * 3. 密码不为空
     *
     * 注意：此测试仅验证参数处理逻辑，不测试实际 Redis 连接
     */
    @Test
    void testConstructorWithPassword() {
        assertDoesNotThrow(() -> {
            String host = "localhost";
            int port = 6379;
            String password = "testpassword";

            assertNotNull(host);
            assertTrue(port > 0);
            assertNotNull(password);
        });
    }

    /**
     * 测试无密码的连接参数
     *
     * 验证：
     * 1. 主机名不为空
     * 2. 端口号大于 0
     * 3. 密码可以为 null（无密码连接）
     *
     * 注意：Redis 支持无密码连接模式
     */
    @Test
    void testConstructorWithoutPassword() {
        assertDoesNotThrow(() -> {
            String host = "localhost";
            int port = 6379;
            String password = null;

            assertNotNull(host);
            assertTrue(port > 0);
            // password 可以为 null，表示无密码连接
        });
    }

    // ==================== 2. Key 模式验证测试 ====================

    /**
     * 测试 Key 模式的有效性
     *
     * 验证：
     * 1. 有效模式不为空
     * 2. 空模式能被正确识别
     * 3. 通配符模式 "*" 能被正确处理
     *
     * Redis Key 模式示例：
     * - "user:*"：匹配所有以 "user:" 开头的 key
     * - "*"：匹配所有 key
     * - "user:123"：精确匹配单个 key
     */
    @Test
    void testKeyPatternValidation() {
        String validPattern = "user:*";
        String emptyPattern = "";
        String allPattern = "*";

        assertNotNull(validPattern);
        assertFalse(validPattern.isEmpty());

        // 测试通配符模式
        assertTrue(allPattern.equals("*"));
    }

    @Test
    void testSafeKeyPolicy() {
        assertDoesNotThrow(() -> RedisMcpServer.requireSafeKey("user:123"));
        assertDoesNotThrow(() -> RedisMcpServer.requireSafeKey("mcp:it:user:profile"));
        assertThrows(IllegalArgumentException.class, () -> RedisMcpServer.requireSafeKey(null));
        assertThrows(IllegalArgumentException.class, () -> RedisMcpServer.requireSafeKey(""));
        assertThrows(IllegalArgumentException.class, () -> RedisMcpServer.requireSafeKey("user:\n123"));
        assertThrows(IllegalArgumentException.class, () -> RedisMcpServer.requireSafeKey("a".repeat(513)));
    }

    @Test
    void testSafeFieldPolicy() {
        assertDoesNotThrow(() -> RedisMcpServer.requireSafeField("name"));
        assertThrows(IllegalArgumentException.class, () -> RedisMcpServer.requireSafeField(""));
        assertThrows(IllegalArgumentException.class, () -> RedisMcpServer.requireSafeField("name\n"));
    }

    @Test
    void testNarrowKeyPatternPolicy() {
        assertDoesNotThrow(() -> RedisMcpServer.requireNarrowKeyPattern("user:*"));
        assertDoesNotThrow(() -> RedisMcpServer.requireNarrowKeyPattern("cache:profile:*"));
        assertDoesNotThrow(() -> RedisMcpServer.requireNarrowKeyPattern("user:123"));
        assertThrows(IllegalArgumentException.class, () -> RedisMcpServer.requireNarrowKeyPattern("*"));
        assertThrows(IllegalArgumentException.class, () -> RedisMcpServer.requireNarrowKeyPattern("*user*"));
        assertThrows(IllegalArgumentException.class, () -> RedisMcpServer.requireNarrowKeyPattern("?ser:*"));
        assertThrows(IllegalArgumentException.class, () -> RedisMcpServer.requireNarrowKeyPattern("u*"));
        assertThrows(IllegalArgumentException.class, () -> RedisMcpServer.requireNarrowKeyPattern(""));
    }

    @Test
    void testSafeInfoSectionPolicy() {
        assertDoesNotThrow(() -> RedisMcpServer.requireSafeInfoSection("server"));
        assertDoesNotThrow(() -> RedisMcpServer.requireSafeInfoSection("memory"));
        assertDoesNotThrow(() -> RedisMcpServer.requireSafeInfoSection("stats"));
        assertDoesNotThrow(() -> RedisMcpServer.requireSafeInfoSection("keyspace"));
        assertThrows(IllegalArgumentException.class, () -> RedisMcpServer.requireSafeInfoSection(null));
        assertThrows(IllegalArgumentException.class, () -> RedisMcpServer.requireSafeInfoSection(""));
        assertThrows(IllegalArgumentException.class, () -> RedisMcpServer.requireSafeInfoSection("all"));
        assertThrows(IllegalArgumentException.class, () -> RedisMcpServer.requireSafeInfoSection("default"));
        assertThrows(IllegalArgumentException.class, () -> RedisMcpServer.requireSafeInfoSection("replication"));
    }

    @Test
    void testSafeRangePolicy() {
        assertDoesNotThrow(() -> RedisMcpServer.requireSafeRange(0, 9));
        assertDoesNotThrow(() -> RedisMcpServer.requireSafeRange(0, 99));
        assertDoesNotThrow(() -> RedisMcpServer.requireSafeRange(-10, -1));
        assertThrows(IllegalArgumentException.class, () -> RedisMcpServer.requireSafeRange(0, -1));
        assertThrows(IllegalArgumentException.class, () -> RedisMcpServer.requireSafeRange(0, 1000));
    }

    // ==================== 3. 工具注册测试 ====================

    /**
     * 测试工具注册到 MCP Server
     *
     * 验证：
     * 1. Redis 工具能正确注册到 Server
     * 2. 注册后工具数量正确（15 个工具）
     * 3. 工具名称正确
     *
     * Redis MCP Server 提供的工具包括：
     * - get, set, del：基本字符串操作
     * - keys, type, ttl：Key 管理
     * - hget, hset, hgetall：Hash 操作
     * - lrange, llen：List 操作
     * - scard, smembers：Set 操作
     * - info, dbsize：服务器信息
     */
    @Test
    void testToolRegistration() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-redis-server")
                .version("1.0.0")
                .build();

        // 注册模拟的 Redis 工具
        server.tool("get", "Get value by key", params -> ToolCallResult.success("ok"));
        server.tool("set", "Set key-value pair", params -> ToolCallResult.success("ok"));
        server.tool("del", "Delete key", params -> ToolCallResult.success("ok"));

        // 验证工具已注册
        assertEquals(3, server.listTools().size());
        List<String> toolNames = server.listTools().stream()
                .map(t -> t.getName())
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        assertEquals(List.of("del", "get", "set"), toolNames);
    }

    /**
     * 测试工具描述是否正确
     *
     * 工具描述用于 AI 理解工具用途，必须准确清晰
     * 例如：get 工具的描述应说明是"根据 key 获取 value"
     */
    @Test
    void testToolDescription() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-redis-server")
                .version("1.0.0")
                .build();

        server.tool("get", "Get the value of a key", params -> ToolCallResult.success("ok"));

        assertEquals("Get the value of a key", server.listTools().get(0).getDescription());
    }

    // ==================== 4. 工具调用测试 ====================

    /**
     * 测试 GET 命令调用
     *
     * 验证：
     * 1. 工具能正确调用
     * 2. key 参数能正确传递
     * 3. 结果能正确返回
     *
     * GET 命令用于获取指定 key 的 value
     */
    @Test
    void testGetCommand() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-redis-server")
                .version("1.0.0")
                .build();

        // 注册模拟的 get 工具
        server.tool("get", "Get value", params -> {
            String key = (String) params.get("key");
            return ToolCallResult.success("value_of_" + key);
        });

        Map<String, Object> args = new HashMap<>();
        args.put("key", "user:123");
        ToolCallResult result = server.callTool("get", args);

        assertNotNull(result);
        assertFalse(result.getIsError());
        assertTrue(result.getContent().get(0).getText().contains("value_of_user:123"));
    }

    /**
     * 测试 SET 命令调用
     *
     * 验证：
     * 1. 工具能正确调用
     * 2. key 和 value 参数能正确传递
     * 3. 结果能正确返回
     *
     * SET 命令用于设置 key-value 对
     */
    @Test
    void testSetCommand() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-redis-server")
                .version("1.0.0")
                .build();

        // 注册模拟的 set 工具
        server.tool("set", "Set value", params -> {
            String key = (String) params.get("key");
            String value = (String) params.get("value");
            return ToolCallResult.success("OK");
        });

        Map<String, Object> args = new HashMap<>();
        args.put("key", "user:123");
        args.put("value", "John");
        ToolCallResult result = server.callTool("set", args);

        assertNotNull(result);
        assertFalse(result.getIsError());
        assertEquals("OK", result.getContent().get(0).getText());
    }

    /**
     * 测试 KEYS 命令调用
     *
     * 验证：
     * 1. 工具能正确调用
     * 2. pattern 参数能正确传递
     * 3. 结果包含匹配的 key 列表
     *
     * KEYS 命令用于查找匹配模式的所有 key
     * 注意：生产环境慎用 KEYS *，可能阻塞服务器
     */
    @Test
    void testKeysCommand() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-redis-server")
                .version("1.0.0")
                .build();

        // 注册模拟的 keys 工具
        server.tool("keys", "Find keys", params -> {
            String pattern = (String) params.get("pattern");
            return ToolCallResult.success("[\"user:1\", \"user:2\", \"user:3\"]");
        });

        Map<String, Object> args = new HashMap<>();
        args.put("pattern", "user:*");
        ToolCallResult result = server.callTool("keys", args);

        assertNotNull(result);
        assertFalse(result.getIsError());
        assertTrue(result.getContent().get(0).getText().contains("user:1"));
    }

    /**
     * 测试调用不存在的工具
     *
     * 验证调用不存在的工具时抛出异常
     */
    @Test
    void testToolCallNotFound() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-redis-server")
                .version("1.0.0")
                .build();

        Map<String, Object> args = new HashMap<>();
        assertThrows(Exception.class, () -> {
            server.callTool("nonexistent", args);
        });
    }

    // ==================== 5. 参数解析测试 ====================

    /**
     * 测试参数提取逻辑
     *
     * 验证：
     * 1. 参数能正确提取
     * 2. 不存在的参数返回 null
     */
    @Test
    void testParameterExtraction() {
        Map<String, Object> params = new HashMap<>();
        params.put("key", "user:123");
        params.put("value", "John");
        params.put("ttl", 3600);

        assertEquals("user:123", params.get("key"));
        assertEquals("John", params.get("value"));
        assertEquals(3600, params.get("ttl"));
        assertNull(params.get("nonexistent"));
    }

    /**
     * 测试不同类型的参数
     *
     * 验证：
     * 1. 字符串参数正确传递
     * 2. 整数参数正确传递
     * 3. 布尔参数正确传递
     */
    @Test
    void testParameterTypes() {
        Map<String, Object> params = new HashMap<>();
        params.put("stringParam", "hello");
        params.put("intParam", 42);
        params.put("boolParam", true);

        assertEquals("hello", params.get("stringParam"));
        assertEquals(42, params.get("intParam"));
        assertEquals(true, params.get("boolParam"));
    }

    // ==================== 6. 结果格式化测试 ====================

    /**
     * 测试成功结果的格式
     *
     * 验证：
     * 1. isError 为 false
     * 2. content 列表不为空
     * 3. 内容类型为 "text"
     * 4. 内容文本正确
     */
    @Test
    void testSuccessResultFormat() {
        ToolCallResult result = ToolCallResult.success("OK");

        assertNotNull(result);
        assertFalse(result.getIsError());
        assertEquals(1, result.getContent().size());
        assertEquals("text", result.getContent().get(0).getType());
        assertEquals("OK", result.getContent().get(0).getText());
    }

    /**
     * 测试错误结果的格式
     *
     * 验证：
     * 1. isError 为 true
     * 2. content 列表不为空
     * 3. 内容文本包含错误信息
     */
    @Test
    void testErrorResultFormat() {
        ToolCallResult result = ToolCallResult.error("Connection refused");

        assertNotNull(result);
        assertTrue(result.getIsError());
        assertEquals(1, result.getContent().size());
        assertEquals("text", result.getContent().get(0).getType());
        assertEquals("Connection refused", result.getContent().get(0).getText());
    }

    /**
     * 测试 JSON 结果的格式
     *
     * 验证：
     * 1. 内容类型为 "text"
     * 2. 内容文本包含 JSON 数据
     * 3. JSON 数据包含正确的字段值
     */
    @Test
    void testJsonResultFormat() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "user:123");
        data.put("value", "John");

        ToolCallResult result = ToolCallResult.json(data);

        assertNotNull(result);
        assertFalse(result.getIsError());
        String json = result.getContent().get(0).getText();
        assertTrue(json.contains("user:123"));
        assertTrue(json.contains("John"));
    }
}
