package com.mcp.server.git;

import com.mcp.annotation.McpServer;
import com.mcp.annotation.McpTool;
import com.mcp.annotation.Param;
import com.mcp.protocol.ToolCallResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Git MCP Server 实现
 *
 * 提供 Git 仓库操作的 MCP 工具，包括：
 * - git_status：查看工作区状态
 * - git_log：查看提交历史
 * - git_diff：查看差异
 * - git_branch：分支管理
 * - git_checkout：切换分支
 * - git_commit：创建提交
 * - git_stash：暂存变更
 * - git_remote：查看远程仓库
 * - git_show：查看提交详情
 * - git_blame：查看文件修改记录
 *
 * 安全特性：
 * - 仅允许安全的 git 子命令
 * - 阻止危险操作（force push、reset --hard 等）
 * - 使用 ProcessBuilder 执行命令，防止 shell 注入
 *
 * @author MCP Java SDK
 */
@McpServer(name = "git-server", version = "1.0.0")
public class GitMcpServer {

    private static final Logger log = LoggerFactory.getLogger(GitMcpServer.class);

    /** Git 仓库根目录 */
    private final File repoDir;

    /**
     * 创建 Git MCP Server
     *
     * @param repoPath Git 仓库路径
     */
    public GitMcpServer(String repoPath) {
        this.repoDir = new File(repoPath).getAbsoluteFile();
        if (!new File(repoDir, ".git").exists()) {
            log.warn("Warning: {} may not be a git repository", repoPath);
        }
        log.info("Git server initialized for repository: {}", repoDir);
    }

    /**
     * 查看工作区状态
     *
     * 执行 git status --short 查看当前工作区的文件变更状态。
     *
     * @return 工作区状态信息
     */
    @McpTool(name = "git_status", description = "Show working tree status")
    public ToolCallResult gitStatus() {
        return executeGitCommand("status", "--short");
    }

    /**
     * 查看提交历史
     *
     * 执行 git log 查看提交历史，支持限制返回数量。
     *
     * @param count 返回的提交数量（可选，默认 10）
     * @return 提交历史列表
     */
    @McpTool(name = "git_log", description = "Show commit logs")
    public ToolCallResult gitLog(
            @Param(name = "count", description = "Number of commits to show (default: 10)", required = false) Integer count
    ) {
        int n = count != null ? count : 10;
        return executeGitCommand("log", "--oneline", "-" + n);
    }

    /**
     * 查看差异
     *
     * 执行 git diff 查看文件差异，支持比较暂存区、工作区或两个提交。
     *
     * @param target 比较目标（可选，如 HEAD~1、branch_name 等）
     * @return 差异信息
     */
    @McpTool(name = "git_diff", description = "Show changes between commits")
    public ToolCallResult gitDiff(
            @Param(name = "target", description = "Comparison target (e.g., HEAD~1, branch_name)", required = false) String target
    ) {
        if (target != null) {
            return executeGitCommand("diff", target);
        }
        return executeGitCommand("diff");
    }

    /**
     * 分支管理
     *
     * 列出、创建或删除分支。
     *
     * @param action 操作类型：list（列出）、create（创建）、delete（删除）
     * @param name   分支名称（创建或删除时必需）
     * @return 操作结果
     */
    @McpTool(name = "git_branch", description = "List, create, or delete branches")
    public ToolCallResult gitBranch(
            @Param(name = "action", description = "Action: list, create, delete") String action,
            @Param(name = "name", description = "Branch name (required for create/delete)", required = false) String name
    ) {
        return switch (action) {
            case "list" -> executeGitCommand("branch", "-a");
            case "create" -> {
                if (name == null) yield ToolCallResult.error("Branch name is required for create");
                yield executeGitCommand("branch", name);
            }
            case "delete" -> {
                if (name == null) yield ToolCallResult.error("Branch name is required for delete");
                yield executeGitCommand("branch", "-d", name);
            }
            default -> ToolCallResult.error("Unknown action: " + action + ". Use list, create, or delete");
        };
    }

    /**
     * 切换分支
     *
     * 执行 git checkout 切换到指定分支。
     *
     * @param branch 目标分支名称
     * @return 操作结果
     */
    @McpTool(name = "git_checkout", description = "Switch branches")
    public ToolCallResult gitCheckout(
            @Param(name = "branch", description = "Branch name to switch to") String branch
    ) {
        if (branch == null || branch.isBlank()) {
            return ToolCallResult.error("Branch name is required");
        }
        return executeGitCommand("checkout", branch);
    }

    /**
     * 创建提交
     *
     * 执行 git commit 创建一个新的提交。
     *
     * @param message 提交信息
     * @return 操作结果
     */
    @McpTool(name = "git_commit", description = "Create a commit")
    public ToolCallResult gitCommit(
            @Param(name = "message", description = "Commit message") String message
    ) {
        if (message == null || message.isBlank()) {
            return ToolCallResult.error("Commit message is required");
        }
        return executeGitCommand("commit", "-m", message);
    }

    /**
     * 暂存变更
     *
     * 执行 git stash 暂存当前工作区的变更。
     *
     * @param action 操作类型：push（暂存）、pop（恢复）、list（列出）、drop（删除）
     * @return 操作结果
     */
    @McpTool(name = "git_stash", description = "Stash or unstash changes")
    public ToolCallResult gitStash(
            @Param(name = "action", description = "Action: push, pop, list, drop", required = false) String action
    ) {
        if (action == null || action.equals("push")) {
            return executeGitCommand("stash", "push");
        }
        return switch (action) {
            case "pop" -> executeGitCommand("stash", "pop");
            case "list" -> executeGitCommand("stash", "list");
            case "drop" -> executeGitCommand("stash", "drop");
            default -> ToolCallResult.error("Unknown action: " + action);
        };
    }

    /**
     * 查看远程仓库
     *
     * 执行 git remote -v 查看远程仓库信息。
     *
     * @return 远程仓库信息
     */
    @McpTool(name = "git_remote", description = "Show remote repository information")
    public ToolCallResult gitRemote() {
        return executeGitCommand("remote", "-v");
    }

    /**
     * 查看提交详情
     *
     * 执行 git show 查看指定提交的详细信息。
     *
     * @param commit 提交哈希或引用（可选，默认 HEAD）
     * @return 提交详情
     */
    @McpTool(name = "git_show", description = "Show commit details")
    public ToolCallResult gitShow(
            @Param(name = "commit", description = "Commit hash or ref (default: HEAD)", required = false) String commit
    ) {
        String ref = commit != null ? commit : "HEAD";
        return executeGitCommand("show", "--stat", ref);
    }

    /**
     * 查看文件修改记录
     *
     * 执行 git blame 查看文件每一行的最后修改信息。
     *
     * @param file 文件路径
     * @return 文件修改记录
     */
    @McpTool(name = "git_blame", description = "Show file blame information")
    public ToolCallResult gitBlame(
            @Param(name = "file", description = "File path") String file
    ) {
        if (file == null || file.isBlank()) {
            return ToolCallResult.error("File path is required");
        }
        return executeGitCommand("blame", file);
    }

    /**
     * 执行 git 命令
     *
     * 使用 ProcessBuilder 执行 git 命令，捕获标准输出和错误输出。
     *
     * @param args git 命令参数
     * @return 命令执行结果
     */
    private ToolCallResult executeGitCommand(String... args) {
        try {
            List<String> command = new ArrayList<>();
            command.add("git");
            command.addAll(Arrays.asList(args));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(repoDir);
            pb.redirectErrorStream(false);

            Process process = pb.start();
            String stdout = new String(process.getInputStream().readAllBytes()).trim();
            String stderr = new String(process.getErrorStream().readAllBytes()).trim();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return ToolCallResult.success(stdout.isEmpty() ? "(no output)" : stdout);
            } else {
                return ToolCallResult.error("Git command failed (exit code " + exitCode + "): " + stderr);
            }

        } catch (Exception e) {
            log.error("Failed to execute git command", e);
            return ToolCallResult.error("Failed to execute git command: " + e.getMessage());
        }
    }
}
