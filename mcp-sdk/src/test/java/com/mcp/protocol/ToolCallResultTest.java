package com.mcp.protocol;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ToolCallResult 单元测试
 */
class ToolCallResultTest {

    @Test
    void testSuccessResult() {
        ToolCallResult result = ToolCallResult.success("Hello, World!");

        assertNotNull(result.getContent());
        assertEquals(1, result.getContent().size());
        assertEquals("text", result.getContent().get(0).getType());
        assertEquals("Hello, World!", result.getContent().get(0).getText());
        assertFalse(result.getIsError());
    }

    @Test
    void testErrorResult() {
        ToolCallResult result = ToolCallResult.error("Something went wrong");

        assertNotNull(result.getContent());
        assertEquals(1, result.getContent().size());
        assertEquals("text", result.getContent().get(0).getType());
        assertEquals("Something went wrong", result.getContent().get(0).getText());
        assertTrue(result.getIsError());
    }

    @Test
    void testJsonResult() {
        // 使用简单的 Map 代替 record 来避免 Jackson 兼容性问题
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("name", "test");
        data.put("value", 42);

        ToolCallResult result = ToolCallResult.json(data);

        assertNotNull(result.getContent());
        assertEquals(1, result.getContent().size());
        assertEquals("text", result.getContent().get(0).getType());
        assertTrue(result.getContent().get(0).getText().contains("test"));
        assertTrue(result.getContent().get(0).getText().contains("42"));
        assertFalse(result.getIsError());
    }

    @Test
    void testTextContent() {
        ToolCallResult.Content content = ToolCallResult.Content.text("test text");

        assertEquals("text", content.getType());
        assertEquals("test text", content.getText());
        assertNull(content.getMimeType());
    }

    @Test
    void testImageContent() {
        ToolCallResult.Content content = ToolCallResult.Content.image("base64data", "image/png");

        assertEquals("image", content.getType());
        assertEquals("base64data", content.getText());
        assertEquals("image/png", content.getMimeType());
    }
}
