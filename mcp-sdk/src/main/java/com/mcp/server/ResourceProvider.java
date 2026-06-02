package com.mcp.server;

import com.mcp.protocol.ResourceContent;

/**
 * 资源提供者接口
 */
@FunctionalInterface
public interface ResourceProvider {

    /**
     * 读取资源内容
     *
     * @param uri 资源 URI
     * @return 资源内容
     */
    ResourceContent read(String uri);
}
