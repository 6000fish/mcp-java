package com.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工具调用结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallResult {

    @JsonProperty("content")
    private List<Content> content;

    @JsonProperty("isError")
    private Boolean isError;

    /**
     * 创建成功结果
     */
    public static ToolCallResult success(String text) {
        return ToolCallResult.builder()
                .content(List.of(Content.text(text)))
                .isError(false)
                .build();
    }

    /**
     * 创建 JSON 结果
     */
    public static ToolCallResult json(Object data) {
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(data);
            return success(json);
        } catch (Exception e) {
            return error("Failed to serialize result: " + e.getMessage());
        }
    }

    /**
     * 创建错误结果
     */
    public static ToolCallResult error(String message) {
        return ToolCallResult.builder()
                .content(List.of(Content.text(message)))
                .isError(true)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        @JsonProperty("type")
        private String type;

        @JsonProperty("text")
        private String text;

        @JsonProperty("mimeType")
        private String mimeType;

        public static Content text(String text) {
            return Content.builder()
                    .type("text")
                    .text(text)
                    .build();
        }

        public static Content image(String base64, String mimeType) {
            return Content.builder()
                    .type("image")
                    .text(base64)
                    .mimeType(mimeType)
                    .build();
        }
    }
}
