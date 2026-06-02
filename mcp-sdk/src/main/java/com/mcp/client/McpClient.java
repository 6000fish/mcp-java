package com.mcp.client;

import com.mcp.protocol.*;
import com.mcp.transport.Transport;

import java.util.List;
import java.util.Map;

/**
 * MCP Client 接口
 */
public interface McpClient {

    /**
     * 连接到 Server
     */
    void connect(Transport transport) throws Exception;

    /**
     * 断开连接
     */
    void disconnect() throws Exception;

    /**
     * 初始化连接
     */
    InitializeResult initialize(InitializeRequest request) throws Exception;

    /**
     * 列出可用工具
     */
    List<Tool> listTools() throws Exception;

    /**
     * 调用工具
     */
    ToolCallResult callTool(String name, Map<String, Object> arguments) throws Exception;

    /**
     * 列出可用资源
     */
    List<Resource> listResources() throws Exception;

    /**
     * 读取资源
     */
    List<ResourceContent> readResource(String uri) throws Exception;

    /**
     * 列出可用 Prompt
     */
    List<Prompt> listPrompts() throws Exception;

    /**
     * 获取 Prompt
     */
    PromptResult getPrompt(String name, Map<String, String> arguments) throws Exception;

    /**
     * 判断是否已连接
     */
    boolean isConnected();
}
