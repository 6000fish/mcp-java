package com.mcp.server.filesystem;

import com.mcp.annotation.McpServer;
import com.mcp.annotation.McpTool;
import com.mcp.annotation.Param;
import com.mcp.protocol.ToolCallResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件系统 MCP Server 实现
 */
@McpServer(name = "filesystem-server", version = "1.0.0")
public class FilesystemMcpServer {

    private static final Logger log = LoggerFactory.getLogger(FilesystemMcpServer.class);

    private final Path rootPath;
    private final Set<Path> allowedPaths;

    public FilesystemMcpServer(String rootDir) {
        this.rootPath = Paths.get(rootDir).toAbsolutePath().normalize();
        this.allowedPaths = new HashSet<>();
        this.allowedPaths.add(rootPath);
        log.info("Filesystem server initialized with root: {}", rootPath);
    }

    public FilesystemMcpServer(String rootDir, List<String> allowedDirs) {
        this.rootPath = Paths.get(rootDir).toAbsolutePath().normalize();
        this.allowedPaths = new HashSet<>();
        this.allowedPaths.add(rootPath);
        for (String dir : allowedDirs) {
            this.allowedPaths.add(Paths.get(dir).toAbsolutePath().normalize());
        }
        log.info("Filesystem server initialized with root: {} and allowed paths: {}", rootPath, allowedPaths);
    }

    @McpTool(name = "read_file", description = "Read the contents of a file")
    public ToolCallResult readFile(
            @Param(name = "path", description = "File path") String path
    ) {
        try {
            Path filePath = validateAndResolvePath(path);

            if (!Files.exists(filePath)) {
                return ToolCallResult.error("File not found: " + path);
            }

            if (!Files.isRegularFile(filePath)) {
                return ToolCallResult.error("Not a regular file: " + path);
            }

            String content = Files.readString(filePath);
            return ToolCallResult.success(content);

        } catch (AccessDeniedException e) {
            return ToolCallResult.error("Access denied: " + path);
        } catch (IOException e) {
            return ToolCallResult.error("Failed to read file: " + e.getMessage());
        }
    }

    @McpTool(name = "write_file", description = "Write content to a file")
    public ToolCallResult writeFile(
            @Param(name = "path", description = "File path") String path,
            @Param(name = "content", description = "Content to write") String content,
            @Param(name = "create_dirs", description = "Create parent directories if not exist", required = false) Boolean createDirs
    ) {
        try {
            Path filePath = validateAndResolvePath(path);

            // 创建父目录
            if (createDirs != null && createDirs) {
                Path parent = filePath.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
            }

            Files.writeString(filePath, content);
            return ToolCallResult.success("File written successfully: " + path);

        } catch (AccessDeniedException e) {
            return ToolCallResult.error("Access denied: " + path);
        } catch (IOException e) {
            return ToolCallResult.error("Failed to write file: " + e.getMessage());
        }
    }

    @McpTool(name = "list_directory", description = "List files and directories in a path")
    public ToolCallResult listDirectory(
            @Param(name = "path", description = "Directory path") String path,
            @Param(name = "pattern", description = "Glob pattern to filter (optional)", required = false) String pattern
    ) {
        try {
            Path dirPath = validateAndResolvePath(path);

            if (!Files.exists(dirPath)) {
                return ToolCallResult.error("Directory not found: " + path);
            }

            if (!Files.isDirectory(dirPath)) {
                return ToolCallResult.error("Not a directory: " + path);
            }

            List<Map<String, Object>> entries = new ArrayList<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath,
                    pattern != null ? pattern : "*")) {
                for (Path entry : stream) {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("name", entry.getFileName().toString());
                    info.put("path", rootPath.relativize(entry).toString());
                    info.put("type", Files.isDirectory(entry) ? "directory" : "file");
                    info.put("size", Files.isRegularFile(entry) ? Files.size(entry) : null);
                    entries.add(info);
                }
            }

            return ToolCallResult.json(entries);

        } catch (AccessDeniedException e) {
            return ToolCallResult.error("Access denied: " + path);
        } catch (IOException e) {
            return ToolCallResult.error("Failed to list directory: " + e.getMessage());
        }
    }

    @McpTool(name = "create_directory", description = "Create a directory")
    public ToolCallResult createDirectory(
            @Param(name = "path", description = "Directory path") String path
    ) {
        try {
            Path dirPath = validateAndResolvePath(path);

            if (Files.exists(dirPath)) {
                if (Files.isDirectory(dirPath)) {
                    return ToolCallResult.success("Directory already exists: " + path);
                }
                return ToolCallResult.error("Path exists but is not a directory: " + path);
            }

            Files.createDirectories(dirPath);
            return ToolCallResult.success("Directory created: " + path);

        } catch (AccessDeniedException e) {
            return ToolCallResult.error("Access denied: " + path);
        } catch (IOException e) {
            return ToolCallResult.error("Failed to create directory: " + e.getMessage());
        }
    }

    @McpTool(name = "delete_file", description = "Delete a file or directory")
    public ToolCallResult deleteFile(
            @Param(name = "path", description = "File or directory path") String path,
            @Param(name = "recursive", description = "Delete directories recursively", required = false) Boolean recursive
    ) {
        try {
            Path targetPath = validateAndResolvePath(path);

            if (!Files.exists(targetPath)) {
                return ToolCallResult.error("Path not found: " + path);
            }

            if (Files.isDirectory(targetPath)) {
                if (recursive != null && recursive) {
                    Files.walkFileTree(targetPath, new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } else {
                    // 检查目录是否为空
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetPath)) {
                        if (stream.iterator().hasNext()) {
                            return ToolCallResult.error("Directory is not empty. Use recursive=true to delete.");
                        }
                    }
                    Files.delete(targetPath);
                }
            } else {
                Files.delete(targetPath);
            }

            return ToolCallResult.success("Deleted: " + path);

        } catch (AccessDeniedException e) {
            return ToolCallResult.error("Access denied: " + path);
        } catch (IOException e) {
            return ToolCallResult.error("Failed to delete: " + e.getMessage());
        }
    }

    @McpTool(name = "move_file", description = "Move or rename a file or directory")
    public ToolCallResult moveFile(
            @Param(name = "source", description = "Source path") String source,
            @Param(name = "destination", description = "Destination path") String destination
    ) {
        try {
            Path sourcePath = validateAndResolvePath(source);
            Path destPath = validateAndResolvePath(destination);

            if (!Files.exists(sourcePath)) {
                return ToolCallResult.error("Source not found: " + source);
            }

            Files.move(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
            return ToolCallResult.success("Moved: " + source + " -> " + destination);

        } catch (AccessDeniedException e) {
            return ToolCallResult.error("Access denied");
        } catch (IOException e) {
            return ToolCallResult.error("Failed to move: " + e.getMessage());
        }
    }

    @McpTool(name = "copy_file", description = "Copy a file or directory")
    public ToolCallResult copyFile(
            @Param(name = "source", description = "Source path") String source,
            @Param(name = "destination", description = "Destination path") String destination
    ) {
        try {
            Path sourcePath = validateAndResolvePath(source);
            Path destPath = validateAndResolvePath(destination);

            if (!Files.exists(sourcePath)) {
                return ToolCallResult.error("Source not found: " + source);
            }

            if (Files.isDirectory(sourcePath)) {
                Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        Path targetDir = destPath.resolve(sourcePath.relativize(dir));
                        Files.createDirectories(targetDir);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.copy(file, destPath.resolve(sourcePath.relativize(file)),
                                StandardCopyOption.REPLACE_EXISTING);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                Files.createDirectories(destPath.getParent());
                Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return ToolCallResult.success("Copied: " + source + " -> " + destination);

        } catch (AccessDeniedException e) {
            return ToolCallResult.error("Access denied");
        } catch (IOException e) {
            return ToolCallResult.error("Failed to copy: " + e.getMessage());
        }
    }

    @McpTool(name = "file_info", description = "Get information about a file or directory")
    public ToolCallResult fileInfo(
            @Param(name = "path", description = "File or directory path") String path
    ) {
        try {
            Path targetPath = validateAndResolvePath(path);

            if (!Files.exists(targetPath)) {
                return ToolCallResult.error("Path not found: " + path);
            }

            BasicFileAttributes attrs = Files.readAttributes(targetPath, BasicFileAttributes.class);

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("path", path);
            info.put("absolutePath", targetPath.toString());
            info.put("type", attrs.isDirectory() ? "directory" : "file");
            info.put("size", attrs.size());
            info.put("created", attrs.creationTime().toString());
            info.put("modified", attrs.lastModifiedTime().toString());
            info.put("accessed", attrs.lastAccessTime().toString());

            return ToolCallResult.json(info);

        } catch (AccessDeniedException e) {
            return ToolCallResult.error("Access denied: " + path);
        } catch (IOException e) {
            return ToolCallResult.error("Failed to get file info: " + e.getMessage());
        }
    }

    @McpTool(name = "search_files", description = "Search for files matching a pattern")
    public ToolCallResult searchFiles(
            @Param(name = "directory", description = "Directory to search in") String directory,
            @Param(name = "pattern", description = "Glob pattern (e.g., *.java)") String pattern,
            @Param(name = "max_depth", description = "Maximum search depth", required = false) Integer maxDepth
    ) {
        try {
            Path dirPath = validateAndResolvePath(directory);

            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                return ToolCallResult.error("Directory not found: " + directory);
            }

            int depth = maxDepth != null ? maxDepth : 10;
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);

            List<String> foundFiles = new ArrayList<>();
            Files.walkFileTree(dirPath, Set.of(), depth, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (matcher.matches(file.getFileName())) {
                        foundFiles.add(rootPath.relativize(file).toString());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            return ToolCallResult.json(foundFiles);

        } catch (AccessDeniedException e) {
            return ToolCallResult.error("Access denied");
        } catch (IOException e) {
            return ToolCallResult.error("Search failed: " + e.getMessage());
        }
    }

    private Path validateAndResolvePath(String pathStr) throws AccessDeniedException {
        Path path = Paths.get(pathStr);

        // 如果是相对路径，基于 rootPath 解析
        if (!path.isAbsolute()) {
            path = rootPath.resolve(path);
        }

        final Path resolvedPath = path.toAbsolutePath().normalize();

        // 安全检查：确保路径在允许的范围内
        boolean isAllowed = allowedPaths.stream().anyMatch(allowed ->
                resolvedPath.startsWith(allowed));

        if (!isAllowed) {
            throw new AccessDeniedException("Path outside allowed directories: " + pathStr);
        }

        return resolvedPath;
    }
}
