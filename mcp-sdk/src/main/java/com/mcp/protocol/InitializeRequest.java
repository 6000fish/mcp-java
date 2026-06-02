package com.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 初始化请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitializeRequest {

    @JsonProperty("protocolVersion")
    private String protocolVersion;

    @JsonProperty("capabilities")
    private ClientCapabilities capabilities;

    @JsonProperty("clientInfo")
    private Implementation clientInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientCapabilities {
        @JsonProperty("roots")
        private RootsCapability roots;

        @JsonProperty("sampling")
        private Object sampling;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RootsCapability {
        @JsonProperty("listChanged")
        private Boolean listChanged;
    }
}
