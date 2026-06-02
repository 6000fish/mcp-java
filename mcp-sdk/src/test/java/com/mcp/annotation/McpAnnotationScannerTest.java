package com.mcp.annotation;

import com.mcp.protocol.PromptResult;
import com.mcp.protocol.ResourceContent;
import com.mcp.protocol.ToolCallResult;
import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * McpAnnotationScanner 单元测试
 *
 * 测试注解扫描器的核心功能：
 * 1. @McpTool 工具注册和调用
 * 2. @McpResource 资源注册和读取
 * 3. @McpPrompt Prompt 注册和获取
 * 4. 参数解析和类型转换
 * 5. 返回值自动包装
 */
class McpAnnotationScannerTest {

    private McpServer server;

    @BeforeEach
    void setUp() {
        server = DefaultMcpServer.builder()
                .name("test-server")
                .version("1.0.0")
                .build();
    }

    // ==================== 1. 工具注册测试 ====================

    /**
     * 测试 @McpTool 注解方法自动注册为工具
     */
    @Test
    void testToolRegistration() {
        McpAnnotationScanner.scan(server, new ToolTestClass());

        List<String> toolNames = server.listTools().stream()
                .map(t -> t.getName())
                .sorted()
                .toList();
        assertEquals(List.of("add", "greet"), toolNames);
    }

    /**
     * 测试工具调用 - 方法返回 String 时自动包装为 ToolCallResult
     */
    @Test
    void testToolCallWithAutoWrap() {
        McpAnnotationScanner.scan(server, new ToolTestClass());

        Map<String, Object> args = new HashMap<>();
        args.put("name", "World");
        ToolCallResult result = server.callTool("greet", args);

        assertNotNull(result);
        assertFalse(result.getIsError());
        assertTrue(result.getContent().get(0).getText().contains("Hello, World"));
    }

    /**
     * 测试工具调用 - 方法返回 ToolCallResult 时直接使用
     */
    @Test
    void testToolCallWithDirectResult() {
        McpAnnotationScanner.scan(server, new ToolTestClass());

        Map<String, Object> args = new HashMap<>();
        args.put("a", 3);
        args.put("b", 5);
        ToolCallResult result = server.callTool("add", args);

        assertNotNull(result);
        assertFalse(result.getIsError());
        assertEquals("8", result.getContent().get(0).getText());
    }

    /**
     * 测试工具调用 - 必填参数缺失时返回错误
     */
    @Test
    void testToolCallMissingRequiredParam() {
        McpAnnotationScanner.scan(server, new ToolTestClass());

        Map<String, Object> args = new HashMap<>();
        // 缺少 name 参数
        ToolCallResult result = server.callTool("greet", args);

        assertNotNull(result);
        assertTrue(result.getIsError());
        assertTrue(result.getContent().get(0).getText().contains("Required parameter missing"));
    }

    // ==================== 2. 资源注册测试 ====================

    /**
     * 测试 @McpResource 注解方法自动注册为资源
     */
    @Test
    void testResourceRegistration() {
        McpAnnotationScanner.scan(server, new ResourceTestClass());

        assertEquals(1, server.listResources().size());
        assertEquals("file:///{path}", server.listResources().get(0).getUri());
    }

    /**
     * 测试资源读取 - URI 模板参数解析
     */
    @Test
    void testResourceRead() {
        McpAnnotationScanner.scan(server, new ResourceTestClass());

        ResourceContent content = server.readResource("file:///etc/hosts");

        assertNotNull(content);
        assertEquals("file:///etc/hosts", content.getUri());
        assertTrue(content.getText().contains("etc/hosts"));
    }

    // ==================== 3. Prompt 注册测试 ====================

    /**
     * 测试 @McpPrompt 注解方法自动注册为 Prompt
     */
    @Test
    void testPromptRegistration() {
        McpAnnotationScanner.scan(server, new PromptTestClass());

        assertEquals(1, server.listPrompts().size());
        assertEquals("code_review", server.listPrompts().get(0).getName());
    }

    /**
     * 测试 Prompt 获取
     */
    @Test
    void testPromptGet() {
        McpAnnotationScanner.scan(server, new PromptTestClass());

        Map<String, String> args = new HashMap<>();
        args.put("language", "Java");
        PromptResult result = server.getPrompt("code_review", args);

        assertNotNull(result);
        assertNotNull(result.getMessages());
        assertFalse(result.getMessages().isEmpty());
        assertTrue(result.getMessages().get(0).getContent().getText().contains("Java"));
    }

    // ==================== 4. 参数类型转换测试 ====================

    /**
     * 测试不同参数类型的自动转换
     */
    @Test
    void testParameterTypeConversion() {
        McpAnnotationScanner.scan(server, new TypeConversionTestClass());

        Map<String, Object> args = new HashMap<>();
        args.put("str", "hello");
        args.put("num", 42);
        args.put("flag", true);
        ToolCallResult result = server.callTool("type_test", args);

        assertNotNull(result);
        assertFalse(result.getIsError());
        String text = result.getContent().get(0).getText();
        assertTrue(text.contains("hello"));
        assertTrue(text.contains("42"));
        assertTrue(text.contains("true"));
    }

    // ==================== 5. 复杂场景测试 ====================

    /**
     * 测试同时包含工具、资源和 Prompt 的类
     */
    @Test
    void testMixedAnnotations() {
        McpAnnotationScanner.scan(server, new MixedTestClass());

        assertEquals(1, server.listTools().size());
        assertEquals(1, server.listResources().size());
        assertEquals(1, server.listPrompts().size());
    }

    // ==================== 测试用内部类 ====================

    @com.mcp.annotation.McpServer(name = "tool-test", version = "1.0.0")
    static class ToolTestClass {

        @McpTool(name = "greet", description = "打招呼")
        public String greet(@Param(name = "name", description = "姓名") String name) {
            return "Hello, " + name;
        }

        @McpTool(name = "add", description = "加法")
        public ToolCallResult add(
                @Param(name = "a", description = "加数") int a,
                @Param(name = "b", description = "被加数") int b) {
            return ToolCallResult.success(String.valueOf(a + b));
        }
    }

    @com.mcp.annotation.McpServer(name = "resource-test", version = "1.0.0")
    static class ResourceTestClass {

        @McpResource(uri = "file:///{path}", name = "readFile", description = "读取文件", mimeType = "text/plain")
        public String readFile(@Param(name = "path") String path) {
            return "Content of " + path;
        }
    }

    @com.mcp.annotation.McpServer(name = "prompt-test", version = "1.0.0")
    static class PromptTestClass {

        @McpPrompt(name = "code_review", description = "代码审查模板")
        public String codeReview(@Param(name = "language") String language) {
            return "Please review this " + language + " code";
        }
    }

    @com.mcp.annotation.McpServer(name = "type-test", version = "1.0.0")
    static class TypeConversionTestClass {

        @McpTool(name = "type_test", description = "类型转换测试")
        public String typeTest(
                @Param(name = "str") String str,
                @Param(name = "num") int num,
                @Param(name = "flag") boolean flag) {
            return str + "|" + num + "|" + flag;
        }
    }

    @com.mcp.annotation.McpServer(name = "mixed-test", version = "1.0.0")
    static class MixedTestClass {

        @McpTool(name = "my_tool", description = "工具")
        public String myTool() {
            return "tool result";
        }

        @McpResource(uri = "config://app", name = "config", description = "配置")
        public String config() {
            return "{}";
        }

        @McpPrompt(name = "my_prompt", description = "提示")
        public String myPrompt() {
            return "prompt content";
        }
    }
}
