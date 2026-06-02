package com.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工具调用结果。
 * <p>
 * 封装 MCP 服务端执行工具调用后的返回结果，包含内容列表和错误标识。
 * 结果可以包含多种类型的内容（文本、图片等），通过 {@link Content} 类描述。
 * </p>
 * <p>
 * 提供便捷的静态工厂方法：
 * <ul>
 *   <li>{@link #success(String)} - 创建文本成功结果</li>
 *   <li>{@link #json(Object)} - 创建 JSON 格式成功结果</li>
 *   <li>{@link #error(String)} - 创建错误结果</li>
 * </ul>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallResult {

    /** 工具调用返回的内容列表，可包含多个文本或图片内容项 */
    @JsonProperty("content")
    private List<Content> content;

    /** 是否为错误结果，true 表示工具执行失败，false 或 null 表示成功 */
    @JsonProperty("isError")
    private Boolean isError;

    /**
     * 创建文本成功结果。
     *
     * @param text 工具执行返回的文本内容
     * @return 包含单个文本内容的成功结果
     */
    public static ToolCallResult success(String text) {
        return ToolCallResult.builder()
                .content(List.of(Content.text(text)))
                .isError(false)
                .build();
    }

    /**
     * 创建 JSON 格式的成功结果。
     * <p>
     * 将对象序列化为 JSON 字符串后作为文本内容返回。序列化失败时返回错误结果。
     * </p>
     *
     * @param data 要序列化为 JSON 的数据对象
     * @return JSON 格式的成功结果，或序列化失败时的错误结果
     */
    public static ToolCallResult json(Object data) {
        try {
            String json = JsonUtils.toJson(data);
            return success(json);
        } catch (Exception e) {
            return error("Failed to serialize result: " + e.getMessage());
        }
    }

    /**
     * 创建错误结果。
     *
     * @param message 错误描述信息
     * @return 标记为错误的结果实例
     */
    public static ToolCallResult error(String message) {
        return ToolCallResult.builder()
                .content(List.of(Content.text(message)))
                .isError(true)
                .build();
    }

    /**
     * 工具调用结果的内容项。
     * <p>
     * 支持文本（text）和图片（image）两种内容类型。
     * 文本内容直接存储在 text 字段中，图片内容以 Base64 编码存储在 text 字段中。
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        /** 内容类型，可选值为 "text"（文本）或 "image"（图片） */
        @JsonProperty("type")
        private String type;

        /** 内容数据：文本类型为纯文本，图片类型为 Base64 编码的图片数据 */
        @JsonProperty("text")
        private String text;

        /** MIME 类型（如 "text/plain"、"image/png"），仅图片类型需要指定 */
        @JsonProperty("mimeType")
        private String mimeType;

        /**
         * 创建文本类型的内容项。
         *
         * @param text 文本内容
         * @return 文本类型的内容项
         */
        public static Content text(String text) {
            return Content.builder()
                    .type("text")
                    .text(text)
                    .build();
        }

        /**
         * 创建图片类型的内容项。
         *
         * @param base64   Base64 编码的图片数据
         * @param mimeType 图片的 MIME 类型（如 "image/png"、"image/jpeg"）
         * @return 图片类型的内容项
         */
        public static Content image(String base64, String mimeType) {
            return Content.builder()
                    .type("image")
                    .text(base64)
                    .mimeType(mimeType)
                    .build();
        }
    }
}
