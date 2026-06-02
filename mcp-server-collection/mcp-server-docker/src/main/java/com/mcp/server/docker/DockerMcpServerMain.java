package com.mcp.server.docker;

import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import com.mcp.transport.StdioTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Docker MCP Server 启动类
 *
 * 环境变量配置：
 * - DOCKER_HOST：Docker 守护进程地址（可选，默认使用本地 socket）
 *
 * @author MCP Java SDK
 */
public class DockerMcpServerMain {

    private static final Logger log = LoggerFactory.getLogger(DockerMcpServerMain.class);

    public static void main(String[] args) throws Exception {
        log.info("Starting Docker MCP Server...");

        // 创建 Docker Server
        DockerMcpServer dockerServer = new DockerMcpServer();

        // 创建 MCP Server
        McpServer server = DefaultMcpServer.builder()
                .name("docker-server")
                .version("1.0.0")
                .build();

        // 注册工具
        server.tool("list_containers", "List containers", params ->
                dockerServer.listContainers((Boolean) params.get("all")));
        server.tool("start_container", "Start a container", params ->
                dockerServer.startContainer((String) params.get("container_id")));
        server.tool("stop_container", "Stop a container", params ->
                dockerServer.stopContainer((String) params.get("container_id")));
        server.tool("restart_container", "Restart a container", params ->
                dockerServer.restartContainer((String) params.get("container_id")));
        server.tool("remove_container", "Remove a container", params ->
                dockerServer.removeContainer((String) params.get("container_id")));
        server.tool("container_logs", "Get container logs", params ->
                dockerServer.containerLogs(
                        (String) params.get("container_id"),
                        params.get("tail") != null ? Integer.parseInt(String.valueOf(params.get("tail"))) : null));
        server.tool("list_images", "List images", params -> dockerServer.listImages());
        server.tool("pull_image", "Pull an image", params ->
                dockerServer.pullImage((String) params.get("image")));
        server.tool("remove_image", "Remove an image", params ->
                dockerServer.removeImage((String) params.get("image")));
        server.tool("inspect_container", "Inspect container", params ->
                dockerServer.inspectContainer((String) params.get("container_id")));
        server.tool("exec_command", "Execute command in container", params ->
                dockerServer.execCommand(
                        (String) params.get("container_id"),
                        (String) params.get("command")));

        // 启动服务
        server.start(new StdioTransport());

        log.info("Docker MCP Server started. Waiting for connections...");

        // 保持运行
        Thread.currentThread().join();
    }
}
