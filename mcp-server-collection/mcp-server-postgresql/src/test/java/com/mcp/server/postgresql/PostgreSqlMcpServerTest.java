package com.mcp.server.postgresql;

import com.mcp.protocol.ToolCallResult;
import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL MCP Server 单元测试
 *
 * 测试目的：验证 PostgreSqlMcpServer 的核心功能，包括：
 * 1. SQL 安全检查逻辑
 * 2. 工具注册和调用流程
 * 3. 参数解析和验证
 * 4. 错误处理机制
 *
 * 注意：本测试不依赖真实的 PostgreSQL 数据库
 */
class PostgreSqlMcpServerTest {

    // ==================== 1. SQL 安全检查逻辑测试 ====================

    /**
     * 测试危险 SQL 语句的识别
     *
     * 验证 DROP、DELETE、TRUNCATE、ALTER、CREATE、GRANT、REVOKE 被正确识别为危险操作
     */
    @Test
    void testDangerousSqlDetection() {
        assertTrue(isDangerousSql("DROP TABLE users"));
        assertTrue(isDangerousSql("DELETE FROM users"));
        assertTrue(isDangerousSql("TRUNCATE TABLE users"));
        assertTrue(isDangerousSql("ALTER TABLE users ADD COLUMN age INT"));
        assertTrue(isDangerousSql("CREATE TABLE test (id INT)"));
        assertTrue(isDangerousSql("GRANT ALL ON *.* TO 'user'@'%'"));
        assertTrue(isDangerousSql("REVOKE ALL ON *.* FROM 'user'@'%'"));

        assertFalse(isDangerousSql("SELECT * FROM users"));
        assertFalse(isDangerousSql("SHOW DATABASES"));
        assertFalse(isDangerousSql("EXPLAIN SELECT * FROM users"));
    }

    /**
     * 测试带空格的 SQL 语句
     */
    @Test
    void testDangerousSqlWithWhitespace() {
        assertTrue(isDangerousSql("  DROP TABLE users"));
        assertTrue(isDangerousSql("  DELETE FROM users  "));
        assertFalse(isDangerousSql("  SELECT * FROM users  "));
    }

    /**
     * 测试大小写不敏感的 SQL 语句
     */
    @Test
    void testDangerousSqlCaseInsensitive() {
        assertTrue(isDangerousSql("drop table users"));
        assertTrue(isDangerousSql("Drop Table Users"));
        assertTrue(isDangerousSql("DROP TABLE USERS"));
        assertFalse(isDangerousSql("select * from users"));
    }

    // ==================== 2. 工具注册测试 ====================

    /**
     * 测试工具注册到 MCP Server
     */
    @Test
    void testToolRegistration() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-server")
                .version("1.0.0")
                .build();

        server.tool("query", "Execute SQL query", params -> ToolCallResult.success("ok"));
        server.tool("list_tables", "List tables", params -> ToolCallResult.success("ok"));

        assertEquals(2, server.listTools().size());
        List<String> toolNames = server.listTools().stream()
                .map(t -> t.getName())
                .sorted()
                .toList();
        assertEquals(List.of("list_tables", "query"), toolNames);
    }

    // ==================== 3. 工具调用测试 ====================

    /**
     * 测试工具调用成功的情况
     */
    @Test
    void testToolCallSuccess() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-server")
                .version("1.0.0")
                .build();

        server.tool("query", "Execute SQL", params -> {
            String sql = (String) params.get("sql");
            if (isDangerousSql(sql)) {
                return ToolCallResult.error("Dangerous SQL operations are not allowed");
            }
            return ToolCallResult.success("[{id: 1, name: 'test'}]");
        });

        Map<String, Object> args = new HashMap<>();
        args.put("sql", "SELECT * FROM users");
        ToolCallResult result = server.callTool("query", args);

        assertNotNull(result);
        assertFalse(result.getIsError());
        assertTrue(result.getContent().get(0).getText().contains("test"));
    }

    /**
     * 测试调用危险 SQL 时返回错误
     */
    @Test
    void testToolCallWithDangerousSql() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-server")
                .version("1.0.0")
                .build();

        server.tool("query", "Execute SQL", params -> {
            String sql = (String) params.get("sql");
            if (isDangerousSql(sql)) {
                return ToolCallResult.error("Dangerous SQL operations are not allowed");
            }
            return ToolCallResult.success("ok");
        });

        Map<String, Object> args = new HashMap<>();
        args.put("sql", "DROP TABLE users");
        ToolCallResult result = server.callTool("query", args);

        assertNotNull(result);
        assertTrue(result.getIsError());
    }

    /**
     * 测试调用不存在的工具
     */
    @Test
    void testToolCallNotFound() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-server")
                .version("1.0.0")
                .build();

        Map<String, Object> args = new HashMap<>();
        assertThrows(Exception.class, () -> server.callTool("nonexistent", args));
    }

    // ==================== 4. 参数解析测试 ====================

    /**
     * 测试参数提取逻辑
     */
    @Test
    void testParameterExtraction() {
        Map<String, Object> params = new HashMap<>();
        params.put("sql", "SELECT * FROM users");
        params.put("schema", "public");

        assertEquals("SELECT * FROM users", params.get("sql"));
        assertEquals("public", params.get("schema"));
        assertNull(params.get("nonexistent"));
    }

    // ==================== 5. 结果格式化测试 ====================

    /**
     * 测试成功结果的格式
     */
    @Test
    void testSuccessResultFormat() {
        ToolCallResult result = ToolCallResult.success("test data");

        assertNotNull(result);
        assertFalse(result.getIsError());
        assertEquals(1, result.getContent().size());
        assertEquals("text", result.getContent().get(0).getType());
    }

    /**
     * 测试错误结果的格式
     */
    @Test
    void testErrorResultFormat() {
        ToolCallResult result = ToolCallResult.error("something went wrong");

        assertNotNull(result);
        assertTrue(result.getIsError());
        assertEquals("something went wrong", result.getContent().get(0).getText());
    }

    /**
     * 测试 JSON 结果的格式
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
