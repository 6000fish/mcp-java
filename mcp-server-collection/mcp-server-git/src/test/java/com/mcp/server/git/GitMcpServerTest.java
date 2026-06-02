package com.mcp.server.git;

import com.mcp.protocol.ToolCallResult;
import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Git MCP Server 单元测试
 *
 * 测试目的：验证 GitMcpServer 的核心功能，包括：
 * 1. Git 命令执行
 * 2. 工具注册和调用流程
 * 3. 参数验证
 * 4. 错误处理机制
 *
 * 注意：使用 @TempDir 创建临时 Git 仓库进行测试
 */
class GitMcpServerTest {

    @TempDir
    Path tempDir;

    // ==================== 1. 工具注册测试 ====================

    /**
     * 测试工具注册到 MCP Server
     */
    @Test
    void testToolRegistration() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-git-server")
                .version("1.0.0")
                .build();

        server.tool("git_status", "Show status", params -> ToolCallResult.success("ok"));
        server.tool("git_log", "Show log", params -> ToolCallResult.success("ok"));
        server.tool("git_diff", "Show diff", params -> ToolCallResult.success("ok"));

        assertEquals(3, server.listTools().size());
        List<String> toolNames = server.listTools().stream()
                .map(t -> t.getName())
                .sorted()
                .toList();
        assertEquals(List.of("git_diff", "git_log", "git_status"), toolNames);
    }

    // ==================== 2. 参数验证测试 ====================

    /**
     * 测试分支名称验证
     */
    @Test
    void testBranchNameValidation() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-git-server")
                .version("1.0.0")
                .build();

        server.tool("git_branch", "Manage branches", params -> {
            String action = (String) params.get("action");
            String name = (String) params.get("name");

            if ("create".equals(action) && (name == null || name.isBlank())) {
                return ToolCallResult.error("Branch name is required for create");
            }
            return ToolCallResult.success("ok");
        });

        Map<String, Object> args = new HashMap<>();
        args.put("action", "create");
        ToolCallResult result = server.callTool("git_branch", args);

        assertNotNull(result);
        assertTrue(result.getIsError());
        assertTrue(result.getContent().get(0).getText().contains("required"));
    }

    /**
     * 测试提交信息验证
     */
    @Test
    void testCommitMessageValidation() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-git-server")
                .version("1.0.0")
                .build();

        server.tool("git_commit", "Create commit", params -> {
            String message = (String) params.get("message");
            if (message == null || message.isBlank()) {
                return ToolCallResult.error("Commit message is required");
            }
            return ToolCallResult.success("ok");
        });

        Map<String, Object> args = new HashMap<>();
        args.put("message", "");
        ToolCallResult result = server.callTool("git_commit", args);

        assertNotNull(result);
        assertTrue(result.getIsError());
    }

    // ==================== 3. 工具调用测试 ====================

    /**
     * 测试工具调用成功的情况
     */
    @Test
    void testToolCallSuccess() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-git-server")
                .version("1.0.0")
                .build();

        server.tool("git_status", "Show status", params ->
                ToolCallResult.success("M file.txt"));

        Map<String, Object> args = new HashMap<>();
        ToolCallResult result = server.callTool("git_status", args);

        assertNotNull(result);
        assertFalse(result.getIsError());
        assertTrue(result.getContent().get(0).getText().contains("file.txt"));
    }

    /**
     * 测试调用不存在的工具
     */
    @Test
    void testToolCallNotFound() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-git-server")
                .version("1.0.0")
                .build();

        Map<String, Object> args = new HashMap<>();
        assertThrows(Exception.class, () -> server.callTool("nonexistent", args));
    }

    // ==================== 4. 错误处理测试 ====================

    /**
     * 测试在非 Git 仓库中执行命令
     */
    @Test
    void testCommandInNonGitRepo() {
        GitMcpServer gitServer = new GitMcpServer(tempDir.toString());

        ToolCallResult result = gitServer.gitStatus();

        // 应该返回错误或空结果（非 git 仓库）
        assertNotNull(result);
    }

    // ==================== 5. 结果格式化测试 ====================

    /**
     * 测试成功结果的格式
     */
    @Test
    void testSuccessResultFormat() {
        ToolCallResult result = ToolCallResult.success("test data");

        assertNotNull(result);
        assertFalse(result.getIsError());
        assertEquals(1, result.getContent().size());
        assertEquals("text", result.getContent().get(0).getType());
    }

    /**
     * 测试错误结果的格式
     */
    @Test
    void testErrorResultFormat() {
        ToolCallResult result = ToolCallResult.error("something went wrong");

        assertNotNull(result);
        assertTrue(result.getIsError());
        assertEquals("something went wrong", result.getContent().get(0).getText());
    }
}
