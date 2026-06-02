package com.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP 工具定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tool {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("inputSchema")
    private InputSchema inputSchema;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InputSchema {
        @JsonProperty("type")
        private String type = "object";

        @JsonProperty("properties")
        private Map<String, Property> properties;

        @JsonProperty("required")
        private java.util.List<String> required;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Property {
        @JsonProperty("type")
        private String type;

        @JsonProperty("description")
        private String description;

        @JsonProperty("enum")
        private java.util.List<String> enumValues;

        @JsonProperty("default")
        private Object defaultValue;
    }
}
