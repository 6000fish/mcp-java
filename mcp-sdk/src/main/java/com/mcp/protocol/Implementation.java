package com.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 实现标识信息。
 * <p>
 * 用于描述客户端或服务端的实现信息，包含名称和版本号。
 * 在初始化握手阶段，客户端和服务端通过此对象互相告知自身的实现详情。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Implementation {

    /** 实现名称（如 "my-mcp-client"、"my-mcp-server"） */
    @JsonProperty("name")
    private String name;

    /** 实现版本号（如 "1.0.0"） */
    @JsonProperty("version")
    private String version;
}
