package com.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MCP Prompt 定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prompt {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("arguments")
    private List<Argument> arguments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Argument {
        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("required")
        private Boolean required;
    }
}
