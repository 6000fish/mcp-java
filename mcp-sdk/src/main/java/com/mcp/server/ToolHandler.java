package com.mcp.server;

import com.mcp.protocol.ToolCallResult;

import java.util.Map;

/**
 * 工具处理器接口
 */
@FunctionalInterface
public interface ToolHandler {

    /**
     * 执行工具调用
     *
     * @param arguments 工具参数
     * @return 执行结果
     */
    ToolCallResult execute(Map<String, Object> arguments);
}
