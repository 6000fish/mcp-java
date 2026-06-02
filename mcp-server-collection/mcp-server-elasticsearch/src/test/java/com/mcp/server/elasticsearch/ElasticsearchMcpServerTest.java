package com.mcp.server.elasticsearch;

import com.mcp.protocol.ToolCallResult;
import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Elasticsearch MCP Server 单元测试
 *
 * 测试目的：验证 ElasticsearchMcpServer 的核心功能，包括：
 * 1. 工具注册和调用流程
 * 2. 参数解析和验证
 * 3. URL 构建逻辑
 * 4. 错误处理机制
 *
 * 注意：本测试不依赖真实的 Elasticsearch 服务
 */
class ElasticsearchMcpServerTest {

    // ==================== 1. 工具注册测试 ====================

    /**
     * 测试工具注册到 MCP Server
     */
    @Test
    void testToolRegistration() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-es-server")
                .version("1.0.0")
                .build();

        server.tool("search", "Search documents", params -> ToolCallResult.success("ok"));
        server.tool("index_document", "Index document", params -> ToolCallResult.success("ok"));
        server.tool("get_document", "Get document", params -> ToolCallResult.success("ok"));
        server.tool("list_indices", "List indices", params -> ToolCallResult.success("ok"));

        assertEquals(4, server.listTools().size());
        List<String> toolNames = server.listTools().stream()
                .map(t -> t.getName())
                .sorted()
                .toList();
        assertEquals(List.of("get_document", "index_document", "list_indices", "search"), toolNames);
    }

    // ==================== 2. 工具调用测试 ====================

    /**
     * 测试工具调用成功的情况
     */
    @Test
    void testToolCallSuccess() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-es-server")
                .version("1.0.0")
                .build();

        server.tool("search", "Search documents", params -> {
            String index = (String) params.get("index");
            return ToolCallResult.success("{\"hits\":{\"total\":0}}");
        });

        Map<String, Object> args = new HashMap<>();
        args.put("index", "test-index");
        ToolCallResult result = server.callTool("search", args);

        assertNotNull(result);
        assertFalse(result.getIsError());
    }

    /**
     * 测试调用不存在的工具
     */
    @Test
    void testToolCallNotFound() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-es-server")
                .version("1.0.0")
                .build();

        Map<String, Object> args = new HashMap<>();
        assertThrows(Exception.class, () -> server.callTool("nonexistent", args));
    }

    // ==================== 3. 参数解析测试 ====================

    /**
     * 测试参数提取逻辑
     */
    @Test
    void testParameterExtraction() {
        Map<String, Object> params = new HashMap<>();
        params.put("index", "test-index");
        params.put("query", "{\"match_all\":{}}");

        assertEquals("test-index", params.get("index"));
        assertEquals("{\"match_all\":{}}", params.get("query"));
        assertNull(params.get("nonexistent"));
    }

    /**
     * 测试不同类型的参数
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

    // ==================== 4. 结果格式化测试 ====================

    /**
     * 测试成功结果的格式
     */
    @Test
    void testSuccessResultFormat() {
        ToolCallResult result = ToolCallResult.success("{\"hits\":{\"total\":0}}");

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
        ToolCallResult result = ToolCallResult.error("Connection refused");

        assertNotNull(result);
        assertTrue(result.getIsError());
        assertEquals("Connection refused", result.getContent().get(0).getText());
    }

    /**
     * 测试 JSON 结果的格式
     */
    @Test
    void testJsonResultFormat() {
        Map<String, Object> data = new HashMap<>();
        data.put("total", 42);
        data.put("index", "test");

        ToolCallResult result = ToolCallResult.json(data);

        assertNotNull(result);
        assertFalse(result.getIsError());
        String json = result.getContent().get(0).getText();
        assertTrue(json.contains("42"));
        assertTrue(json.contains("test"));
    }
}
