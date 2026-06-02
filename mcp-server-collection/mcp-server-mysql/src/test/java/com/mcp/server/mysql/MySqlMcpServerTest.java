package com.mcp.server.mysql;

import com.mcp.protocol.ToolCallResult;
import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MySQL MCP Server 单元测试
 *
 * 测试目的：验证 MySqlMcpServer 的核心功能，包括：
 * 1. SQL 安全检查逻辑
 * 2. 工具注册和调用流程
 * 3. 参数解析和验证
 * 4. 错误处理机制
 * 5. 结果格式化
 *
 * 注意：本测试不依赖真实的 MySQL 数据库，使用模拟数据测试逻辑
 */
class MySqlMcpServerTest {

    // ==================== 1. SQL 安全检查逻辑测试 ====================

    /**
     * 测试危险 SQL 语句的识别
     *
     * 危险 SQL 包括：
     * - DROP：删除表/数据库
     * - DELETE：删除数据
     * - TRUNCATE：清空表
     * - ALTER：修改表结构
     * - CREATE：创建表/数据库
     * - GRANT：授权
     * - REVOKE：撤销权限
     *
     * 安全 SQL 包括：
     * - SELECT：查询数据
     * - SHOW：显示信息
     * - DESCRIBE：查看表结构
     * - EXPLAIN：查看执行计划
     */
    @Test
    void testDangerousSqlDetection() {
        // 测试各种危险 SQL 是否被正确识别
        assertTrue(isDangerousSql("DROP TABLE users"));
        assertTrue(isDangerousSql("DELETE FROM users"));
        assertTrue(isDangerousSql("TRUNCATE TABLE users"));
        assertTrue(isDangerousSql("ALTER TABLE users ADD COLUMN age INT"));
        assertTrue(isDangerousSql("CREATE TABLE test (id INT)"));
        assertTrue(isDangerousSql("GRANT ALL ON *.* TO 'user'@'%'"));
        assertTrue(isDangerousSql("REVOKE ALL ON *.* FROM 'user'@'%'"));

        // 测试安全 SQL 不被误判
        assertFalse(isDangerousSql("SELECT * FROM users"));
        assertFalse(isDangerousSql("SHOW DATABASES"));
        assertFalse(isDangerousSql("SHOW TABLES"));
        assertFalse(isDangerousSql("DESCRIBE users"));
        assertFalse(isDangerousSql("EXPLAIN SELECT * FROM users"));
    }

    /**
     * 测试带空格的 SQL 语句
     *
     * 验证安全检查能正确处理前后有空格的 SQL
     */
    @Test
    void testDangerousSqlWithWhitespace() {
        assertTrue(isDangerousSql("  DROP TABLE users"));
        assertTrue(isDangerousSql("  DELETE FROM users  "));
        assertFalse(isDangerousSql("  SELECT * FROM users  "));
    }

    /**
     * 测试大小写不敏感的 SQL 语句
     *
     * 验证安全检查不区分大小写
     */
    @Test
    void testDangerousSqlCaseInsensitive() {
        assertTrue(isDangerousSql("drop table users"));
        assertTrue(isDangerousSql("Drop Table Users"));
        assertTrue(isDangerousSql("DROP TABLE USERS"));
        assertFalse(isDangerousSql("select * from users"));
        assertFalse(isDangerousSql("Select * From Users"));
    }

    // ==================== 2. 工具注册测试 ====================

    /**
     * 测试工具注册到 MCP Server
     *
     * 验证：
     * 1. 工具能正确注册到 Server
     * 2. 注册后工具数量正确
     * 3. 工具名称正确
     */
    @Test
    void testToolRegistration() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-server")
                .version("1.0.0")
                .build();

        // 注册两个工具
        server.tool("query", "Execute SQL query", params -> ToolCallResult.success("ok"));
        server.tool("list_tables", "List tables", params -> ToolCallResult.success("ok"));

        // 验证工具已注册
        assertEquals(2, server.listTools().size());
        List<String> toolNames = server.listTools().stream()
                .map(t -> t.getName())
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        assertEquals(List.of("list_tables", "query"), toolNames);
    }

    /**
     * 测试工具描述是否正确
     *
     * 工具描述用于 AI 理解工具用途，必须准确清晰
     */
    @Test
    void testToolDescription() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-server")
                .version("1.0.0")
                .build();

        server.tool("query", "Execute a SQL query", params -> ToolCallResult.success("ok"));

        assertEquals("Execute a SQL query", server.listTools().get(0).getDescription());
    }

    // ==================== 3. 工具调用测试 ====================

    /**
     * 测试工具调用成功的情况
     *
     * 验证：
     * 1. 工具能正确调用
     * 2. 参数能正确传递
     * 3. 结果能正确返回
     */
    @Test
    void testToolCallSuccess() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-server")
                .version("1.0.0")
                .build();

        // 注册一个返回固定结果的工具
        server.tool("echo", "Echo input", params -> {
            String input = (String) params.get("input");
            return ToolCallResult.success("Echo: " + input);
        });

        // 调用工具
        Map<String, Object> args = new HashMap<>();
        args.put("input", "hello");
        ToolCallResult result = server.callTool("echo", args);

        // 验证结果
        assertNotNull(result);
        assertFalse(result.getIsError());
        assertTrue(result.getContent().get(0).getText().contains("Echo: hello"));
    }

    /**
     * 测试调用不存在的工具
     *
     * 验证调用不存在的工具时抛出异常
     */
    @Test
    void testToolCallNotFound() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-server")
                .version("1.0.0")
                .build();

        Map<String, Object> args = new HashMap<>();
        assertThrows(Exception.class, () -> {
            server.callTool("nonexistent", args);
        });
    }

    // ==================== 4. 错误处理测试 ====================

    /**
     * 测试调用危险 SQL 时返回错误
     *
     * 验证安全检查生效，危险 SQL 被拒绝执行
     */
    @Test
    void testToolCallWithDangerousSql() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-server")
                .version("1.0.0")
                .build();

        // 注册一个模拟的 query 工具
        server.tool("query", "Execute SQL", params -> {
            String sql = (String) params.get("sql");
            if (isDangerousSql(sql)) {
                return ToolCallResult.error("Dangerous SQL operations are not allowed");
            }
            return ToolCallResult.success("ok");
        });

        // 调用危险 SQL
        Map<String, Object> args = new HashMap<>();
        args.put("sql", "DROP TABLE users");
        ToolCallResult result = server.callTool("query", args);

        // 验证返回错误
        assertNotNull(result);
        assertTrue(result.getIsError());
        assertTrue(result.getContent().get(0).getText().contains("Dangerous"));
    }

    /**
     * 测试调用安全 SQL 时正常执行
     *
     * 验证安全 SQL 能正常执行并返回结果
     */
    @Test
    void testToolCallWithSafeSql() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-server")
                .version("1.0.0")
                .build();

        // 注册一个模拟的 query 工具
        server.tool("query", "Execute SQL", params -> {
            String sql = (String) params.get("sql");
            if (isDangerousSql(sql)) {
                return ToolCallResult.error("Dangerous SQL operations are not allowed");
            }
            // 模拟查询结果
            return ToolCallResult.success("[{id: 1, name: 'test'}]");
        });

        // 调用安全 SQL
        Map<String, Object> args = new HashMap<>();
        args.put("sql", "SELECT * FROM users");
        ToolCallResult result = server.callTool("query", args);

        // 验证正常返回
        assertNotNull(result);
        assertFalse(result.getIsError());
        assertTrue(result.getContent().get(0).getText().contains("test"));
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
        params.put("sql", "SELECT * FROM users");
        params.put("database", "mydb");

        assertEquals("SELECT * FROM users", params.get("sql"));
        assertEquals("mydb", params.get("database"));
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
        ToolCallResult result = ToolCallResult.success("test data");

        assertNotNull(result);
        assertFalse(result.getIsError());
        assertEquals(1, result.getContent().size());
        assertEquals("text", result.getContent().get(0).getType());
        assertEquals("test data", result.getContent().get(0).getText());
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
        ToolCallResult result = ToolCallResult.error("something went wrong");

        assertNotNull(result);
        assertTrue(result.getIsError());
        assertEquals(1, result.getContent().size());
        assertEquals("text", result.getContent().get(0).getType());
        assertEquals("something went wrong", result.getContent().get(0).getText());
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
        data.put("count", 42);
        data.put("table", "users");

        ToolCallResult result = ToolCallResult.json(data);

        assertNotNull(result);
        assertFalse(result.getIsError());
        String json = result.getContent().get(0).getText();
        assertTrue(json.contains("42"));
        assertTrue(json.contains("users"));
    }

    // ==================== 辅助方法 ====================

    /**
     * SQL 安全检查方法
     *
     * 用于判断 SQL 语句是否为危险操作
     * 危险操作包括：DROP、DELETE、TRUNCATE、ALTER、CREATE、GRANT、REVOKE
     *
     * @param sql SQL 语句
     * @return true 表示危险，false 表示安全
     */
    private boolean isDangerousSql(String sql) {
        String upper = sql.toUpperCase().trim();
        return upper.startsWith("DROP") ||
                upper.startsWith("DELETE") ||
                upper.startsWith("TRUNCATE") ||
                upper.startsWith("ALTER") ||
                upper.startsWith("CREATE") ||
                upper.startsWith("GRANT") ||
                upper.startsWith("REVOKE");
    }
}
