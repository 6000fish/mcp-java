package com.mcp.server.filesystem;

import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import com.mcp.transport.StdioTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Filesystem MCP Server 启动类
 */
public class FilesystemMcpServerMain {

    private static final Logger log = LoggerFactory.getLogger(FilesystemMcpServerMain.class);

    public static void main(String[] args) throws Exception {
        // 从环境变量读取配置
        String rootDir = getEnvOrDefault("FILESYSTEM_ROOT", ".");
        String allowedDirsStr = getEnvOrDefault("FILESYSTEM_ALLOWED_DIRS", "");

        log.info("Starting Filesystem MCP Server...");
        log.info("Root directory: {}", rootDir);

        // 解析允许的目录
        List<String> allowedDirs = allowedDirsStr.isEmpty() ?
                List.of() :
                Arrays.asList(allowedDirsStr.split(","));

        // 创建 Filesystem Server
        FilesystemMcpServer filesystemServer = new FilesystemMcpServer(rootDir, allowedDirs);

        // 创建 MCP Server
        McpServer server = DefaultMcpServer.builder()
                .name("filesystem-server")
                .version("1.0.0")
                .build();

        // 注册工具
        server.tool("read_file", "Read file contents", params ->
                filesystemServer.readFile((String) params.get("path")));
        server.tool("write_file", "Write content to file", params ->
                filesystemServer.writeFile(
                        (String) params.get("path"),
                        (String) params.get("content"),
                        params.get("create_dirs") != null ? Boolean.parseBoolean(String.valueOf(params.get("create_dirs"))) : null));
        server.tool("list_directory", "List directory contents", params ->
                filesystemServer.listDirectory(
                        (String) params.get("path"),
                        (String) params.get("pattern")));
        server.tool("create_directory", "Create a directory", params ->
                filesystemServer.createDirectory((String) params.get("path")));
        server.tool("delete_file", "Delete file or directory", params ->
                filesystemServer.deleteFile(
                        (String) params.get("path"),
                        params.get("recursive") != null ? Boolean.parseBoolean(String.valueOf(params.get("recursive"))) : null));
        server.tool("move_file", "Move or rename file", params ->
                filesystemServer.moveFile(
                        (String) params.get("source"),
                        (String) params.get("destination")));
        server.tool("copy_file", "Copy file or directory", params ->
                filesystemServer.copyFile(
                        (String) params.get("source"),
                        (String) params.get("destination")));
        server.tool("file_info", "Get file information", params ->
                filesystemServer.fileInfo((String) params.get("path")));
        server.tool("search_files", "Search for files", params ->
                filesystemServer.searchFiles(
                        (String) params.get("directory"),
                        (String) params.get("pattern"),
                        params.get("max_depth") != null ? Integer.parseInt(String.valueOf(params.get("max_depth"))) : null));

        // 启动服务
        server.start(new StdioTransport());

        log.info("Filesystem MCP Server started. Waiting for connections...");

        // 保持运行
        Thread.currentThread().join();
    }

    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }
}
