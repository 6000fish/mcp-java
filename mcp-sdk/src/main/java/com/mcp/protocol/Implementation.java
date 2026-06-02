package com.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 实现信息（客户端/服务端标识）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Implementation {

    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private String version;
}
