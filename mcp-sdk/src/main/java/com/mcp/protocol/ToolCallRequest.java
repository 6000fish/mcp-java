package com.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP 工具调用请求。
 * <p>
 * 客户端通过 {@code tools/call} 方法调用服务端提供的工具时使用的请求参数对象。
 * 包含要调用的工具名称和传入的参数键值对。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallRequest {

    /** 要调用的工具名称，必须与 {@code tools/list} 返回的工具名称一致 */
    @JsonProperty("name")
    private String name;

    /** 工具调用参数，键值对形式，键为参数名，值为参数值 */
    @JsonProperty("arguments")
    private Map<String, Object> arguments;
}
