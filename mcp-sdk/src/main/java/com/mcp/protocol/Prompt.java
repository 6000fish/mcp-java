package com.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MCP 提示模板定义。
 * <p>
 * 描述 MCP 服务端提供的一个可复用的提示模板。
 * 提示模板包含名称、描述和参数列表，客户端通过 {@code prompts/get} 方法获取模板实例化后的消息内容。
 * </p>
 * <p>
 * 提示模板是 MCP 协议的核心概念之一，允许服务端向 LLM 提供结构化的提示内容，
 * 从而引导 LLM 生成符合预期的回复。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prompt {

    /** 提示模板的唯一标识名称 */
    @JsonProperty("name")
    private String name;

    /** 提示模板的人类可读描述，说明模板的用途和适用场景 */
    @JsonProperty("description")
    private String description;

    /** 提示模板的参数定义列表，描述模板接受的输入参数 */
    @JsonProperty("arguments")
    private List<Argument> arguments;

    /**
     * 提示模板的参数定义。
     * <p>
     * 描述提示模板接受的单个输入参数的名称、描述和是否必填。
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Argument {
        /** 参数名称 */
        @JsonProperty("name")
        private String name;

        /** 参数的人类可读描述，说明参数的含义和用法 */
        @JsonProperty("description")
        private String description;

        /** 是否为必填参数，true 表示必须提供，false 或 null 表示可选 */
        @JsonProperty("required")
        private Boolean required;
    }
}
