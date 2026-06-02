package com.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP 工具定义。
 * <p>
 * 描述 MCP 服务端提供的一个可调用工具的元数据信息，包括工具名称、功能描述和输入参数的 JSON Schema 定义。
 * 客户端通过 {@code tools/list} 方法获取可用工具列表，再通过 {@code tools/call} 方法调用指定工具。
 * </p>
 * <p>
 * 每个工具包含：
 * <ul>
 *   <li>name - 工具的唯一标识名称</li>
 *   <li>description - 工具功能的人类可读描述</li>
 *   <li>inputSchema - 输入参数的 JSON Schema 定义，描述参数类型、必填项等约束</li>
 * </ul>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tool {

    /** 工具的唯一标识名称，调用时使用此名称（如 "read_file"、"search_docs"） */
    @JsonProperty("name")
    private String name;

    /** 工具功能的人类可读描述，帮助 LLM 理解工具的用途和适用场景 */
    @JsonProperty("description")
    private String description;

    /** 输入参数的 JSON Schema 定义，描述工具接受的参数结构和约束 */
    @JsonProperty("inputSchema")
    private InputSchema inputSchema;

    /**
     * 输入参数的 JSON Schema 定义。
     * <p>
     * 遵循 JSON Schema 规范，描述工具输入参数的结构。
     * 默认类型为 "object"，通过 properties 定义各参数的类型和约束。
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InputSchema {
        /** Schema 类型，默认为 "object" */
        @JsonProperty("type")
        private String type = "object";

        /** 参数属性定义，键为参数名，值为参数的 Schema 描述 */
        @JsonProperty("properties")
        private Map<String, Property> properties;

        /** 必填参数名称列表 */
        @JsonProperty("required")
        private java.util.List<String> required;
    }

    /**
     * 单个参数属性的 Schema 描述。
     * <p>
     * 定义参数的数据类型、描述信息、可选枚举值和默认值等约束。
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Property {
        /** 参数数据类型（如 "string"、"number"、"boolean"、"array" 等） */
        @JsonProperty("type")
        private String type;

        /** 参数的人类可读描述，帮助 LLM 理解参数含义 */
        @JsonProperty("description")
        private String description;

        /** 参数的可选枚举值列表，限定参数只能取其中的值 */
        @JsonProperty("enum")
        private java.util.List<String> enumValues;

        /** 参数的默认值，当调用方未提供该参数时使用 */
        @JsonProperty("default")
        private Object defaultValue;
    }
}
