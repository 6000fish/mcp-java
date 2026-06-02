package com.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MCP 提示获取结果。
 * <p>
 * 服务端对客户端 {@code prompts/get} 请求的响应，包含提示模板实例化后的消息列表。
 * 每条消息包含角色（role）和内容（content），可直接用于 LLM 对话上下文。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptResult {

    /** 提示模板实例化后的描述信息 */
    @JsonProperty("description")
    private String description;

    /** 提示模板实例化后的消息列表，可直接用于 LLM 对话上下文 */
    @JsonProperty("messages")
    private List<Message> messages;

    /**
     * 提示消息。
     * <p>
     * 表示 LLM 对话中的一条消息，包含角色和内容。
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        /** 消息角色，可选值为 "user"（用户）、"assistant"（助手）、"system"（系统） */
        @JsonProperty("role")
        private String role;

        /** 消息内容 */
        @JsonProperty("content")
        private Content content;
    }

    /**
     * 消息内容。
     * <p>
     * 描述消息的具体内容，目前支持文本类型。
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        /** 内容类型，当前固定为 "text" */
        @JsonProperty("type")
        private String type;

        /** 文本内容 */
        @JsonProperty("text")
        private String text;

        /**
         * 创建文本内容实例。
         *
         * @param text 文本内容
         * @return 文本内容实例
         */
        public static Content text(String text) {
            return Content.builder()
                    .type("text")
                    .text(text)
                    .build();
        }
    }
}
