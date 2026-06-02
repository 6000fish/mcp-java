package com.mcp.server.git;

import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import com.mcp.transport.StdioTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Git MCP Server 启动类
 *
 * 环境变量配置：
 * - GIT_REPO_PATH：Git 仓库路径（默认当前目录）
 *
 * @author MCP Java SDK
 */
public class GitMcpServerMain {

    private static final Logger log = LoggerFactory.getLogger(GitMcpServerMain.class);

    public static void main(String[] args) throws Exception {
        // 从环境变量读取配置
        String repoPath = getEnvOrDefault("GIT_REPO_PATH", ".");

        log.info("Starting Git MCP Server...");
        log.info("Repository path: {}", repoPath);

        // 创建 Git Server
        GitMcpServer gitServer = new GitMcpServer(repoPath);

        // 创建 MCP Server
        McpServer server = DefaultMcpServer.builder()
                .name("git-server")
                .version("1.0.0")
                .build();

        // 注册工具
        server.tool("git_status", "Show working tree status", params -> gitServer.gitStatus());
        server.tool("git_log", "Show commit logs", params ->
                gitServer.gitLog(params.get("count") != null ?
                        Integer.parseInt(String.valueOf(params.get("count"))) : null));
        server.tool("git_diff", "Show changes", params ->
                gitServer.gitDiff((String) params.get("target")));
        server.tool("git_branch", "Manage branches", params ->
                gitServer.gitBranch(
                        (String) params.get("action"),
                        (String) params.get("name")));
        server.tool("git_checkout", "Switch branches", params ->
                gitServer.gitCheckout((String) params.get("branch")));
        server.tool("git_commit", "Create a commit", params ->
                gitServer.gitCommit((String) params.get("message")));
        server.tool("git_stash", "Stash changes", params ->
                gitServer.gitStash((String) params.get("action")));
        server.tool("git_remote", "Show remote info", params -> gitServer.gitRemote());
        server.tool("git_show", "Show commit details", params ->
                gitServer.gitShow((String) params.get("commit")));
        server.tool("git_blame", "Show file blame", params ->
                gitServer.gitBlame((String) params.get("file")));

        // 启动服务
        server.start(new StdioTransport());

        log.info("Git MCP Server started. Waiting for connections...");

        // 保持运行
        Thread.currentThread().join();
    }

    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }
}
