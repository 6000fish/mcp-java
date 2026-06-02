package com.mcp.server;

import com.mcp.protocol.PromptResult;

import java.util.Map;

/**
 * Prompt 处理器接口（函数式接口）
 * <p>
 * MCP 服务器端的核心扩展点之一。实现此接口可注册自定义 Prompt 模板，
 * 当客户端通过 MCP 协议发起 {@code prompts/get} 请求时，
 * 服务器会调用对应 PromptHandler 的 {@link #execute} 方法来生成 Prompt 内容。
 * </p>
 * <p>
 * Prompt 模板允许客户端获取结构化的提示词，通常用于引导 AI 模型完成特定任务。
 * 作为 {@code @FunctionalInterface}，支持 Lambda 表达式简化注册：
 * <pre>
 * server.prompt("code-review", "代码审查模板", args -&gt; new PromptResult(...));
 * </pre>
 * </p>
 *
 * @see McpServer#prompt(String, String, PromptHandler)
 */
@FunctionalInterface
public interface PromptHandler {

    /**
     * 执行 Prompt 模板，生成 Prompt 内容
     * <p>
     * 接收客户端传入的参数，填充 Prompt 模板并返回结构化的提示词结果。
     * 参数通常包含模板变量的值（如编程语言、代码片段等）。
     * </p>
     *
     * @param arguments Prompt 模板参数，键为参数名，值为参数值（均为字符串类型）
     * @return Prompt 执行结果，包含生成的消息列表
     */
    PromptResult execute(Map<String, String> arguments);
}
