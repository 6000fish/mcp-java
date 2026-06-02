package com.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MCP 初始化响应。
 * <p>
 * 服务端对客户端 {@code initialize} 请求的响应，包含协商后的协议版本、
 * 服务端能力声明、服务端标识信息以及可选的操作指引说明。
 * </p>
 * <p>
 * 客户端收到此响应后，应发送 {@code notifications/initialized} 通知告知服务端初始化完成，
 * 随后即可开始正式的 MCP 通信。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitializeResult {

    /** 协商后的 MCP 协议版本号，客户端应使用此版本进行后续通信 */
    @JsonProperty("protocolVersion")
    private String protocolVersion;

    /** 服务端支持的能力声明，告知客户端服务端提供哪些功能 */
    @JsonProperty("capabilities")
    private ServerCapabilities capabilities;

    /** 服务端标识信息，包含服务端名称和版本号 */
    @JsonProperty("serverInfo")
    private Implementation serverInfo;

    /** 可选的操作指引说明，向客户端提供使用服务端的指导信息 */
    @JsonProperty("instructions")
    private String instructions;

    /**
     * 服务端能力声明。
     * <p>
     * 描述服务端支持的 MCP 扩展能力，客户端据此决定可以使用哪些功能。
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServerCapabilities {
        /** 工具能力，服务端是否提供可调用的工具 */
        @JsonProperty("tools")
        private ToolsCapability tools;

        /** 资源能力，服务端是否提供可读取的资源 */
        @JsonProperty("resources")
        private ResourcesCapability resources;

        /** 提示能力，服务端是否提供提示模板 */
        @JsonProperty("prompts")
        private PromptsCapability prompts;

        /** 日志能力，服务端是否支持日志级别设置和日志消息推送 */
        @JsonProperty("logging")
        private Object logging;

        /** 补全能力，服务端是否支持参数自动补全 */
        @JsonProperty("completions")
        private Object completions;
    }

    /**
     * 工具能力声明。
     * <p>
     * 描述服务端对工具列表变更通知的支持情况。
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolsCapability {
        /** 是否支持工具列表变更通知，为 true 时服务端会在工具列表变化时通知客户端 */
        @JsonProperty("listChanged")
        private Boolean listChanged;
    }

    /**
     * 资源能力声明。
     * <p>
     * 描述服务端对资源订阅和变更通知的支持情况。
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourcesCapability {
        /** 是否支持资源订阅，为 true 时客户端可以订阅单个资源的变更通知 */
        @JsonProperty("subscribe")
        private Boolean subscribe;

        /** 是否支持资源列表变更通知，为 true 时服务端会在资源列表变化时通知客户端 */
        @JsonProperty("listChanged")
        private Boolean listChanged;
    }

    /**
     * 提示能力声明。
     * <p>
     * 描述服务端对提示模板列表变更通知的支持情况。
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromptsCapability {
        /** 是否支持提示列表变更通知，为 true 时服务端会在提示模板列表变化时通知客户端 */
        @JsonProperty("listChanged")
        private Boolean listChanged;
    }
}
