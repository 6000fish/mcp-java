package com.mcp.server;

import com.mcp.protocol.ResourceContent;

/**
 * 资源提供者接口（函数式接口）
 * <p>
 * MCP 服务器端的核心扩展点之一。实现此接口可注册自定义资源，
 * 当客户端通过 MCP 协议发起 {@code resources/read} 请求时，
 * 服务器会调用对应 ResourceProvider 的 {@link #read} 方法来读取资源内容。
 * </p>
 * <p>
 * 资源通过 URI 进行唯一标识，支持文件、数据库记录、API 端点等多种数据源。
 * 作为 {@code @FunctionalInterface}，支持 Lambda 表达式简化注册：
 * <pre>
 * server.resource("file:///config", "配置文件", "应用配置", uri -&gt; new ResourceContent(...));
 * </pre>
 * </p>
 *
 * @see McpServer#resource(String, String, String, ResourceProvider)
 */
@FunctionalInterface
public interface ResourceProvider {

    /**
     * 读取资源内容
     * <p>
     * 根据给定的 URI 读取并返回资源内容。实现方需根据 URI 解析数据源，
     * 并将内容封装为 {@link ResourceContent} 返回。
     * </p>
     *
     * @param uri 资源的唯一标识 URI（如 {@code "file:///path/to/file"}）
     * @return 资源内容，包含 URI、MIME 类型和实际内容
     */
    ResourceContent read(String uri);
}
