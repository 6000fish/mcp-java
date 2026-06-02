package com.mcp.protocol;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ResourceContent 单元测试
 */
class ResourceContentTest {

    @Test
    void testTextContent() {
        ResourceContent content = ResourceContent.text("file:///test.txt", "Hello");

        assertEquals("file:///test.txt", content.getUri());
        assertEquals("text/plain", content.getMimeType());
        assertEquals("Hello", content.getText());
        assertNull(content.getBlob());
    }

    @Test
    void testJsonContent() {
        // 使用简单的 Map 代替 record 来避免 Jackson 兼容性问题
        Map<String, String> data = new java.util.LinkedHashMap<>();
        data.put("key", "test");
        data.put("value", "123");

        ResourceContent content = ResourceContent.json("file:///data.json", data);

        assertEquals("file:///data.json", content.getUri());
        assertEquals("application/json", content.getMimeType());
        assertNotNull(content.getText());
        assertTrue(content.getText().contains("test"));
        assertTrue(content.getText().contains("123"));
    }

    @Test
    void testBlobContent() {
        ResourceContent content = ResourceContent.blob("file:///image.png", "base64data", "image/png");

        assertEquals("file:///image.png", content.getUri());
        assertEquals("image/png", content.getMimeType());
        assertEquals("base64data", content.getBlob());
        assertNull(content.getText());
    }
}
