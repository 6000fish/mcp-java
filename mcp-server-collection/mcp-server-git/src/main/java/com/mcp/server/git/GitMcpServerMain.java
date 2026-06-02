package com.mcp.server.git;

import com.mcp.annotation.McpAnnotationScanner;
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

        // 通过注解自动注册工具
        McpAnnotationScanner.scan(server, gitServer);

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
