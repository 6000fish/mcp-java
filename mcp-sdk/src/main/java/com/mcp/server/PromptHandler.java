package com.mcp.server;

import com.mcp.protocol.PromptResult;

import java.util.Map;

/**
 * Prompt 处理器接口
 */
@FunctionalInterface
public interface PromptHandler {

    /**
     * 获取 Prompt 内容
     *
     * @param arguments Prompt 参数
     * @return Prompt 结果
     */
    PromptResult execute(Map<String, String> arguments);
}
