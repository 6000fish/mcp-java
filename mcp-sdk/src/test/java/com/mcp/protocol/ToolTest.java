package com.mcp.protocol;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP 工具定义单元测试
 *
 * 测试目的：验证 Tool 类的构建和属性设置功能
 * 测试范围：工具定义、输入模式、参数属性的构建和验证
 */
class ToolTest {

    /**
     * 测试工具定义的完整构建
     *
     * 工具定义包含：
     * 1. name：工具名称，用于调用时的标识
     * 2. description：工具描述，用于 AI 理解工具用途
     * 3. inputSchema：输入参数的 JSON Schema 定义
     *
     * 验证点：
     * 1. 所有属性正确设置
     * 2. 输入模式包含正确的参数定义
     * 3. 必填参数列表正确
     */
    @Test
    void testToolBuilder() {
        Tool tool = Tool.builder()
                .name("test_tool")
                .description("A test tool")
                .inputSchema(Tool.InputSchema.builder()
                        .type("object")
                        .properties(Map.of(
                                "param1", Tool.Property.builder()
                                        .type("string")
                                        .description("First parameter")
                                        .build(),
                                "param2", Tool.Property.builder()
                                        .type("integer")
                                        .description("Second parameter")
                                        .build()
                        ))
                        .required(List.of("param1"))
                        .build())
                .build();

        assertEquals("test_tool", tool.getName());
        assertEquals("A test tool", tool.getDescription());
        assertNotNull(tool.getInputSchema());
        assertEquals("object", tool.getInputSchema().getType());
        assertEquals(2, tool.getInputSchema().getProperties().size());
        assertEquals(1, tool.getInputSchema().getRequired().size());
    }

    /**
     * 测试参数属性的构建
     *
     * 参数属性包含：
     * 1. type：参数类型（string、integer、boolean 等）
     * 2. description：参数描述
     * 3. enumValues：枚举值列表（可选）
     * 4. defaultValue：默认值（可选）
     *
     * 验证点：
     * 1. 所有属性正确设置
     * 2. 枚举值列表完整
     * 3. 默认值正确
     */
    @Test
    void testToolProperty() {
        Tool.Property property = Tool.Property.builder()
                .type("string")
                .description("A test property")
                .enumValues(List.of("value1", "value2"))
                .defaultValue("value1")
                .build();

        assertEquals("string", property.getType());
        assertEquals("A test property", property.getDescription());
        assertEquals(2, property.getEnumValues().size());
        assertEquals("value1", property.getDefaultValue());
    }
}
