package com.mcp.protocol;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 资源内容单元测试
 *
 * 测试目的：验证 ResourceContent 类的内容创建功能
 * 测试范围：文本内容、JSON 内容、二进制内容的创建
 */
class ResourceContentTest {

    /**
     * 测试创建文本内容
     *
     * 文本内容用于返回纯文本资源，如配置文件、代码等
     *
     * 验证点：
     * 1. URI 正确设置
     * 2. MIME 类型为 "text/plain"
     * 3. 文本内容正确
     * 4. blob 字段为空（文本内容不使用）
     */
    @Test
    void testTextContent() {
        ResourceContent content = ResourceContent.text("file:///test.txt", "Hello");

        assertEquals("file:///test.txt", content.getUri());
        assertEquals("text/plain", content.getMimeType());
        assertEquals("Hello", content.getText());
        assertNull(content.getBlob());
    }

    /**
     * 测试创建 JSON 内容
     *
     * JSON 内容用于返回结构化数据资源，会自动序列化为 JSON 字符串
     *
     * 验证点：
     * 1. URI 正确设置
     * 2. MIME 类型为 "application/json"
     * 3. 文本内容包含 JSON 数据
     * 4. JSON 数据包含正确的字段值
     */
    @Test
    void testJsonContent() {
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

    /**
     * 测试创建二进制内容
     *
     * 二进制内容用于返回图片、文件等二进制资源（Base64 编码）
     *
     * 验证点：
     * 1. URI 正确设置
     * 2. MIME 类型正确
     * 3. blob 字段包含 Base64 数据
     * 4. text 字段为空（二进制内容不使用）
     */
    @Test
    void testBlobContent() {
        ResourceContent content = ResourceContent.blob("file:///image.png", "base64data", "image/png");

        assertEquals("file:///image.png", content.getUri());
        assertEquals("image/png", content.getMimeType());
        assertEquals("base64data", content.getBlob());
        assertNull(content.getText());
    }
}
