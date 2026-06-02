package com.mcp.protocol;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP 提示定义单元测试
 *
 * 测试目的：验证 Prompt 和 PromptResult 类的构建功能
 * 测试范围：提示定义、提示参数、提示结果、消息内容的构建和验证
 */
class PromptTest {

    /**
     * 测试提示定义的构建
     *
     * 提示是预定义的对话模板，包含：
     * 1. name：提示名称，用于调用时的标识
     * 2. description：提示描述，用于 AI 理解提示用途
     * 3. arguments：提示参数列表
     *
     * 验证点：
     * 1. 所有属性正确设置
     * 2. 参数列表完整
     * 3. 参数属性正确
     */
    @Test
    void testPromptBuilder() {
        Prompt prompt = Prompt.builder()
                .name("code_review")
                .description("Review code for best practices")
                .arguments(List.of(
                        Prompt.Argument.builder()
                                .name("language")
                                .description("Programming language")
                                .required(true)
                                .build(),
                        Prompt.Argument.builder()
                                .name("code")
                                .description("Code to review")
                                .required(true)
                                .build()
                ))
                .build();

        assertEquals("code_review", prompt.getName());
        assertEquals("Review code for best practices", prompt.getDescription());
        assertNotNull(prompt.getArguments());
        assertEquals(2, prompt.getArguments().size());
        assertEquals("language", prompt.getArguments().get(0).getName());
        assertTrue(prompt.getArguments().get(0).getRequired());
    }

    /**
     * 测试提示结果的构建
     *
     * 提示结果包含完整的对话消息列表：
     * 1. description：提示描述
     * 2. messages：消息列表，每个消息包含 role 和 content
     *
     * 验证点：
     * 1. 描述正确设置
     * 2. 消息列表完整
     * 3. 消息角色正确（system、user、assistant）
     * 4. 消息内容正确
     */
    @Test
    void testPromptResult() {
        PromptResult result = PromptResult.builder()
                .description("Code review prompt")
                .messages(List.of(
                        PromptResult.Message.builder()
                                .role("system")
                                .content(PromptResult.Content.text("You are a code reviewer."))
                                .build(),
                        PromptResult.Message.builder()
                                .role("user")
                                .content(PromptResult.Content.text("Review this Java code: ..."))
                                .build()
                ))
                .build();

        assertEquals("Code review prompt", result.getDescription());
        assertEquals(2, result.getMessages().size());
        assertEquals("system", result.getMessages().get(0).getRole());
        assertEquals("user", result.getMessages().get(1).getRole());
    }

    /**
     * 测试提示结果内容的创建
     *
     * 消息内容是对话的基本单位，包含：
     * 1. type：内容类型（目前只有 "text"）
     * 2. text：文本内容
     *
     * 验证点：
     * 1. 类型为 "text"
     * 2. 文本内容正确
     */
    @Test
    void testPromptResultContent() {
        PromptResult.Content content = PromptResult.Content.text("Hello, World!");

        assertEquals("text", content.getType());
        assertEquals("Hello, World!", content.getText());
    }
}
