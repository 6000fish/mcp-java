package com.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 初始化请求。
 * <p>
 * 客户端向服务端发送的第一个请求，用于建立连接并协商双方的能力。
 * 该请求对应 MCP 协议中的 {@code initialize} 方法。
 * </p>
 * <p>
 * 请求中包含：
 * <ul>
 *   <li>协议版本号 - 用于协商双方支持的协议版本</li>
 *   <li>客户端能力声明 - 告知服务端客户端支持的功能（如根目录、采样等）</li>
 *   <li>客户端标识信息 - 客户端的名称和版本号</li>
 * </ul>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitializeRequest {

    /** 客户端支持的 MCP 协议版本号 */
    @JsonProperty("protocolVersion")
    private String protocolVersion;

    /** 客户端支持的能力声明，告知服务端客户端具备哪些功能 */
    @JsonProperty("capabilities")
    private ClientCapabilities capabilities;

    /** 客户端标识信息，包含客户端名称和版本号 */
    @JsonProperty("clientInfo")
    private Implementation clientInfo;

    /**
     * 客户端能力声明。
     * <p>
     * 描述客户端支持的 MCP 扩展能力，服务端据此调整自身行为。
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientCapabilities {
        /** 根目录能力，声明客户端是否支持文件系统根目录列表功能 */
        @JsonProperty("roots")
        private RootsCapability roots;

        /** 采样能力，声明客户端是否支持服务端发起的 LLM 采样请求 */
        @JsonProperty("sampling")
        private Object sampling;
    }

    /**
     * 根目录能力声明。
     * <p>
     * 描述客户端对文件系统根目录功能的支持情况。
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RootsCapability {
        /** 是否支持根目录列表变更通知，为 true 时客户端会在根目录变化时通知服务端 */
        @JsonProperty("listChanged")
        private Boolean listChanged;
    }
}
