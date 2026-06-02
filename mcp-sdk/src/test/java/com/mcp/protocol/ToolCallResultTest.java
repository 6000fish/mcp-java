package com.mcp.protocol;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工具调用结果单元测试
 *
 * 测试目的：验证 ToolCallResult 类的结果创建功能
 * 测试范围：成功结果、错误结果、JSON 结果、文本内容、图片内容的创建
 */
class ToolCallResultTest {

    /**
     * 测试创建成功结果
     *
     * 成功结果用于返回工具执行的成功响应
     *
     * 验证点：
     * 1. content 列表不为空
     * 2. content 列表包含一个元素
     * 3. 内容类型为 "text"
     * 4. 内容文本正确
     * 5. isError 为 false
     */
    @Test
    void testSuccessResult() {
        ToolCallResult result = ToolCallResult.success("Hello, World!");

        assertNotNull(result.getContent());
        assertEquals(1, result.getContent().size());
        assertEquals("text", result.getContent().get(0).getType());
        assertEquals("Hello, World!", result.getContent().get(0).getText());
        assertFalse(result.getIsError());
    }

    /**
     * 测试创建错误结果
     *
     * 错误结果用于返回工具执行的错误信息
     *
     * 验证点：
     * 1. content 列表不为空
     * 2. 内容类型为 "text"
     * 3. 内容文本包含错误信息
     * 4. isError 为 true
     */
    @Test
    void testErrorResult() {
        ToolCallResult result = ToolCallResult.error("Something went wrong");

        assertNotNull(result.getContent());
        assertEquals(1, result.getContent().size());
        assertEquals("text", result.getContent().get(0).getType());
        assertEquals("Something went wrong", result.getContent().get(0).getText());
        assertTrue(result.getIsError());
    }

    /**
     * 测试创建 JSON 结果
     *
     * JSON 结果用于返回结构化数据，会自动序列化为 JSON 字符串
     *
     * 验证点：
     * 1. 内容类型为 "text"
     * 2. 内容文本包含 JSON 数据
     * 3. JSON 数据包含正确的字段值
     * 4. isError 为 false
     */
    @Test
    void testJsonResult() {
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

    /**
     * 测试创建文本内容
     *
     * 文本内容是最基本的内容类型，用于返回纯文本信息
     *
     * 验证点：
     * 1. 类型为 "text"
     * 2. 文本内容正确
     * 3. mimeType 为空（文本类型不需要）
     */
    @Test
    void testTextContent() {
        ToolCallResult.Content content = ToolCallResult.Content.text("test text");

        assertEquals("text", content.getType());
        assertEquals("test text", content.getText());
        assertNull(content.getMimeType());
    }

    /**
     * 测试创建图片内容
     *
     * 图片内容用于返回图片数据（Base64 编码）
     *
     * 验证点：
     * 1. 类型为 "image"
     * 2. 文本字段包含 Base64 数据
     * 3. mimeType 为图片类型
     */
    @Test
    void testImageContent() {
        ToolCallResult.Content content = ToolCallResult.Content.image("base64data", "image/png");

        assertEquals("image", content.getType());
        assertEquals("base64data", content.getText());
        assertEquals("image/png", content.getMimeType());
    }
}
