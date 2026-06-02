package com.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MCP 初始化响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitializeResult {

    @JsonProperty("protocolVersion")
    private String protocolVersion;

    @JsonProperty("capabilities")
    private ServerCapabilities capabilities;

    @JsonProperty("serverInfo")
    private Implementation serverInfo;

    @JsonProperty("instructions")
    private String instructions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerCapabilities {
        @JsonProperty("tools")
        private ToolsCapability tools;

        @JsonProperty("resources")
        private ResourcesCapability resources;

        @JsonProperty("prompts")
        private PromptsCapability prompts;

        @JsonProperty("logging")
        private Object logging;

        @JsonProperty("completions")
        private Object completions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolsCapability {
        @JsonProperty("listChanged")
        private Boolean listChanged;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourcesCapability {
        @JsonProperty("subscribe")
        private Boolean subscribe;

        @JsonProperty("listChanged")
        private Boolean listChanged;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromptsCapability {
        @JsonProperty("listChanged")
        private Boolean listChanged;
    }
}
