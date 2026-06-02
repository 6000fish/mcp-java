package com.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 资源定义。
 * <p>
 * 描述 MCP 服务端提供的一个可读取资源的元数据信息。
 * 资源通过唯一 URI 标识，客户端可通过 {@code resources/read} 方法读取资源内容。
 * </p>
 * <p>
 * 典型的资源包括：文件内容、数据库查询结果、API 响应数据等。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    /** 资源的唯一标识 URI（如 "file:///path/to/file"、"db://table/row"） */
    @JsonProperty("uri")
    private String uri;

    /** 资源的人类可读名称，用于在用户界面中展示 */
    @JsonProperty("name")
    private String name;

    /** 资源的功能描述，帮助 LLM 理解资源的内容和用途 */
    @JsonProperty("description")
    private String description;

    /** 资源内容的 MIME 类型（如 "text/plain"、"application/json"） */
    @JsonProperty("mimeType")
    private String mimeType;
}
