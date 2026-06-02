package com.mcp.protocol;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prompt 单元测试
 */
class PromptTest {

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

    @Test
    void testPromptResultContent() {
        PromptResult.Content content = PromptResult.Content.text("Hello, World!");

        assertEquals("text", content.getType());
        assertEquals("Hello, World!", content.getText());
    }
}
