package com.mcp.protocol;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tool 单元测试
 */
class ToolTest {

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
