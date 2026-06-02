package com.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 资源内容
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceContent {

    @JsonProperty("uri")
    private String uri;

    @JsonProperty("mimeType")
    private String mimeType;

    @JsonProperty("text")
    private String text;

    @JsonProperty("blob")
    private String blob;

    /**
     * 创建文本内容
     */
    public static ResourceContent text(String uri, String text) {
        return ResourceContent.builder()
                .uri(uri)
                .mimeType("text/plain")
                .text(text)
                .build();
    }

    /**
     * 创建 JSON 内容
     */
    public static ResourceContent json(String uri, Object data) {
        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(data);
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
     * 创建二进制内容
     */
    public static ResourceContent blob(String uri, String base64, String mimeType) {
        return ResourceContent.builder()
                .uri(uri)
                .mimeType(mimeType)
                .blob(base64)
                .build();
    }
}
