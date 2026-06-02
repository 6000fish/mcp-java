package com.mcp.server;

import com.mcp.protocol.*;
import com.mcp.transport.Transport;

import java.util.List;
import java.util.Map;

/**
 * MCP Server 接口
 */
public interface McpServer {

    /**
     * 注册工具
     */
    McpServer tool(String name, String description, ToolHandler handler);

    /**
     * 注册资源
     */
    McpServer resource(String uri, String name, String description, ResourceProvider provider);

    /**
     * 注册 Prompt
     */
    McpServer prompt(String name, String description, PromptHandler handler);

    /**
     * 启动服务
     */
    void start(Transport transport) throws Exception;

    /**
     * 停止服务
     */
    void stop() throws Exception;

    /**
     * 获取已注册的工具列表
     */
    List<Tool> listTools();

    /**
     * 获取已注册的资源列表
     */
    List<Resource> listResources();

    /**
     * 获取已注册的 Prompt 列表
     */
    List<Prompt> listPrompts();

    /**
     * 调用工具
     */
    ToolCallResult callTool(String name, Map<String, Object> arguments);

    /**
     * 读取资源
     */
    ResourceContent readResource(String uri);

    /**
     * 获取 Prompt
     */
    PromptResult getPrompt(String name, Map<String, String> arguments);
}
