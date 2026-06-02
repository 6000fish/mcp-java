package com.mcp.server.docker;

import com.mcp.annotation.McpServer;
import com.mcp.annotation.McpTool;
import com.mcp.annotation.Param;
import com.mcp.protocol.ToolCallResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Docker MCP Server 实现
 *
 * 提供 Docker 容器管理的 MCP 工具，包括：
 * - list_containers：列出容器
 * - start_container：启动容器
 * - stop_container：停止容器
 * - restart_container：重启容器
 * - remove_container：删除容器
 * - container_logs：获取容器日志
 * - list_images：列出镜像
 * - pull_image：拉取镜像
 * - remove_image：删除镜像
 * - inspect_container：查看容器详情
 * - exec_command：在容器中执行命令
 *
 * 安全特性：
 * - 输入验证，阻止 shell 注入
 * - 阻止危险操作（--privileged、system prune 等）
 * - 使用 ProcessBuilder 执行命令
 * - 命令执行超时 30 秒
 *
 * @author MCP Java SDK
 */
@McpServer(name = "docker-server", version = "1.0.0")
public class DockerMcpServer {

    private static final Logger log = LoggerFactory.getLogger(DockerMcpServer.class);

    /** 命令执行超时时间（秒） */
    private static final int TIMEOUT_SECONDS = 30;

    /**
     * 列出容器
     *
     * 执行 docker ps 列出容器，默认只列出运行中的容器。
     *
     * @param all 是否列出所有容器（包括已停止的）
     * @return 容器列表
     */
    @McpTool(name = "list_containers", description = "List containers")
    public ToolCallResult listContainers(
            @Param(name = "all", description = "Show all containers (including stopped)", required = false) Boolean all
    ) {
        if (all != null && all) {
            return executeDockerCommand("ps", "-a");
        }
        return executeDockerCommand("ps");
    }

    /**
     * 启动容器
     *
     * 执行 docker start 启动指定容器。
     *
     * @param containerId 容器 ID 或名称
     * @return 操作结果
     */
    @McpTool(name = "start_container", description = "Start a container")
    public ToolCallResult startContainer(
            @Param(name = "container_id", description = "Container ID or name") String containerId
    ) {
        if (!isValidId(containerId)) {
            return ToolCallResult.error("Invalid container ID");
        }
        return executeDockerCommand("start", containerId);
    }

    /**
     * 停止容器
     *
     * 执行 docker stop 停止指定容器。
     *
     * @param containerId 容器 ID 或名称
     * @return 操作结果
     */
    @McpTool(name = "stop_container", description = "Stop a container")
    public ToolCallResult stopContainer(
            @Param(name = "container_id", description = "Container ID or name") String containerId
    ) {
        if (!isValidId(containerId)) {
            return ToolCallResult.error("Invalid container ID");
        }
        return executeDockerCommand("stop", containerId);
    }

    /**
     * 重启容器
     *
     * 执行 docker restart 重启指定容器。
     *
     * @param containerId 容器 ID 或名称
     * @return 操作结果
     */
    @McpTool(name = "restart_container", description = "Restart a container")
    public ToolCallResult restartContainer(
            @Param(name = "container_id", description = "Container ID or name") String containerId
    ) {
        if (!isValidId(containerId)) {
            return ToolCallResult.error("Invalid container ID");
        }
        return executeDockerCommand("restart", containerId);
    }

    /**
     * 删除容器
     *
     * 执行 docker rm 删除指定容器。
     *
     * @param containerId 容器 ID 或名称
     * @return 操作结果
     */
    @McpTool(name = "remove_container", description = "Remove a container")
    public ToolCallResult removeContainer(
            @Param(name = "container_id", description = "Container ID or name") String containerId
    ) {
        if (!isValidId(containerId)) {
            return ToolCallResult.error("Invalid container ID");
        }
        return executeDockerCommand("rm", containerId);
    }

    /**
     * 获取容器日志
     *
     * 执行 docker logs 获取指定容器的日志输出。
     *
     * @param containerId 容器 ID 或名称
     * @param tail        返回的日志行数（可选，默认 100）
     * @return 容器日志
     */
    @McpTool(name = "container_logs", description = "Get container logs")
    public ToolCallResult containerLogs(
            @Param(name = "container_id", description = "Container ID or name") String containerId,
            @Param(name = "tail", description = "Number of lines to show (default: 100)", required = false) Integer tail
    ) {
        if (!isValidId(containerId)) {
            return ToolCallResult.error("Invalid container ID");
        }
        int lines = tail != null ? tail : 100;
        return executeDockerCommand("logs", "--tail", String.valueOf(lines), containerId);
    }

    /**
     * 列出镜像
     *
     * 执行 docker images 列出本地镜像。
     *
     * @return 镜像列表
     */
    @McpTool(name = "list_images", description = "List images")
    public ToolCallResult listImages() {
        return executeDockerCommand("images");
    }

    /**
     * 拉取镜像
     *
     * 执行 docker pull 从远程仓库拉取镜像。
     *
     * @param image 镜像名称（如 nginx:latest）
     * @return 拉取结果
     */
    @McpTool(name = "pull_image", description = "Pull an image")
    public ToolCallResult pullImage(
            @Param(name = "image", description = "Image name (e.g., nginx:latest)") String image
    ) {
        if (!isValidImageName(image)) {
            return ToolCallResult.error("Invalid image name");
        }
        return executeDockerCommand("pull", image);
    }

    /**
     * 删除镜像
     *
     * 执行 docker rmi 删除本地镜像。
     *
     * @param image 镜像名称或 ID
     * @return 删除结果
     */
    @McpTool(name = "remove_image", description = "Remove an image")
    public ToolCallResult removeImage(
            @Param(name = "image", description = "Image name or ID") String image
    ) {
        if (!isValidImageName(image)) {
            return ToolCallResult.error("Invalid image name");
        }
        return executeDockerCommand("rmi", image);
    }

    /**
     * 查看容器详情
     *
     * 执行 docker inspect 获取容器的详细信息。
     *
     * @param containerId 容器 ID 或名称
     * @return 容器详情
     */
    @McpTool(name = "inspect_container", description = "Get container details")
    public ToolCallResult inspectContainer(
            @Param(name = "container_id", description = "Container ID or name") String containerId
    ) {
        if (!isValidId(containerId)) {
            return ToolCallResult.error("Invalid container ID");
        }
        return executeDockerCommand("inspect", containerId);
    }

    /**
     * 在容器中执行命令
     *
     * 执行 docker exec 在运行中的容器中执行命令。
     *
     * @param containerId 容器 ID 或名称
     * @param command     要执行的命令
     * @return 命令执行结果
     */
    @McpTool(name = "exec_command", description = "Execute command in container")
    public ToolCallResult execCommand(
            @Param(name = "container_id", description = "Container ID or name") String containerId,
            @Param(name = "command", description = "Command to execute") String command
    ) {
        if (!isValidId(containerId)) {
            return ToolCallResult.error("Invalid container ID");
        }
        if (isDangerousCommand(command)) {
            return ToolCallResult.error("Dangerous command is not allowed");
        }
        return executeDockerCommand("exec", containerId, "sh", "-c", command);
    }

    /**
     * 执行 Docker 命令
     *
     * 使用 ProcessBuilder 执行 docker 命令。
     *
     * @param args 命令参数
     * @return 命令执行结果
     */
    private ToolCallResult executeDockerCommand(String... args) {
        try {
            List<String> command = new ArrayList<>();
            command.add("docker");
            command.addAll(Arrays.asList(args));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);

            Process process = pb.start();
            String stdout = new String(process.getInputStream().readAllBytes()).trim();
            String stderr = new String(process.getErrorStream().readAllBytes()).trim();

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ToolCallResult.error("Command timed out after " + TIMEOUT_SECONDS + " seconds");
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return ToolCallResult.success(stdout.isEmpty() ? "(no output)" : stdout);
            } else {
                return ToolCallResult.error("Docker command failed (exit code " + exitCode + "): " + stderr);
            }

        } catch (Exception e) {
            log.error("Failed to execute docker command", e);
            return ToolCallResult.error("Failed to execute docker command: " + e.getMessage());
        }
    }

    /**
     * 验证容器 ID 是否有效
     *
     * 容器 ID 只能包含字母、数字、短横线和下划线。
     *
     * @param id 容器 ID
     * @return true 表示有效
     */
    private boolean isValidId(String id) {
        return id != null && id.matches("^[a-zA-Z0-9_-]+$");
    }

    /**
     * 验证镜像名称是否有效
     *
     * 阻止包含 shell 元字符的镜像名称。
     *
     * @param imageName 镜像名称
     * @return true 表示有效
     */
    private boolean isValidImageName(String imageName) {
        if (imageName == null || imageName.isBlank()) return false;
        // 阻止 shell 注入
        return !imageName.matches(".*[;&|`$(){}].*");
    }

    /**
     * 检查是否为危险命令
     *
     * 阻止以下危险操作：
     * - --privileged：特权模式
     * - --network host：主机网络
     * - system prune：清理系统
     * - -v /:：挂载根目录
     *
     * @param command 命令字符串
     * @return true 表示危险
     */
    private boolean isDangerousCommand(String command) {
        if (command == null) return false;
        String lower = command.toLowerCase();
        return lower.contains("--privileged") ||
                lower.contains("--network host") ||
                lower.contains("system prune") ||
                lower.contains("-v /:") ||
                lower.contains("rm -rf /");
    }
}
