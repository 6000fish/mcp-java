package com.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 资源内容。
 * <p>
 * 封装通过 {@code resources/read} 方法获取的资源内容数据。
 * 支持三种内容形式：
 * <ul>
 *   <li>纯文本内容 - 通过 {@link #text(String, String)} 创建</li>
 *   <li>JSON 内容 - 通过 {@link #json(String, Object)} 创建</li>
 *   <li>二进制内容 - 通过 {@link #blob(String, String, String)} 创建，以 Base64 编码存储</li>
 * </ul>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceContent {

    /** 资源的唯一标识 URI */
    @JsonProperty("uri")
    private String uri;

    /** 资源内容的 MIME 类型（如 "text/plain"、"application/json"、"image/png"） */
    @JsonProperty("mimeType")
    private String mimeType;

    /** 文本形式的资源内容，适用于文本和 JSON 类型的资源 */
    @JsonProperty("text")
    private String text;

    /** 二进制形式的资源内容，以 Base64 编码存储，适用于图片、文件等二进制资源 */
    @JsonProperty("blob")
    private String blob;

    /**
     * 创建纯文本类型的资源内容。
     *
     * @param uri  资源的唯一标识 URI
     * @param text 纯文本内容
     * @return 文本类型的资源内容实例
     */
    public static ResourceContent text(String uri, String text) {
        return ResourceContent.builder()
                .uri(uri)
                .mimeType("text/plain")
                .text(text)
                .build();
    }

    /**
     * 创建 JSON 类型的资源内容。
     * <p>
     * 将对象序列化为 JSON 字符串后存储。序列化失败时降级为纯文本错误信息。
     * </p>
     *
     * @param uri  资源的唯一标识 URI
     * @param data 要序列化为 JSON 的数据对象
     * @return JSON 类型的资源内容实例
     */
    public static ResourceContent json(String uri, Object data) {
        try {
            String json = JsonUtils.toJson(data);
            return ResourceContent.builder()
                    .uri(uri)
                    .mimeType("application/json")
                    .text(json)
                    .build();
        } catch (Exception e) {
            return text(uri, "Error: " + e.getMessage());
        }
    }

    /**
     * 创建二进制类型的资源内容。
     *
     * @param uri      资源的唯一标识 URI
     * @param base64   Base64 编码的二进制数据
     * @param mimeType 二进制数据的 MIME 类型（如 "image/png"、"application/pdf"）
     * @return 二进制类型的资源内容实例
     */
    public static ResourceContent blob(String uri, String base64, String mimeType) {
        return ResourceContent.builder()
                .uri(uri)
                .mimeType(mimeType)
                .blob(base64)
                .build();
    }
}
