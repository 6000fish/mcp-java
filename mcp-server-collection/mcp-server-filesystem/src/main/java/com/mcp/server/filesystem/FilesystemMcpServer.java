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
 * 文件系统 MCP Server 实现。
 * <p>
 * 本类通过 MCP（Model Context Protocol）协议将文件系统操作暴露为可调用的工具，
 * 使 AI 模型能够安全地与本地文件系统进行交互。
 * </p>
 * <p>
 * 提供以下 MCP 工具：
 * <ul>
 *   <li><b>read_file</b> - 读取文件内容</li>
 *   <li><b>write_file</b> - 写入文件内容，支持自动创建父目录</li>
 *   <li><b>list_directory</b> - 列出目录内容，支持 glob 模式过滤</li>
 *   <li><b>create_directory</b> - 创建目录（含父目录）</li>
 *   <li><b>delete_file</b> - 删除文件或目录，支持递归删除</li>
 *   <li><b>move_file</b> - 移动或重命名文件/目录</li>
 *   <li><b>copy_file</b> - 复制文件或目录</li>
 *   <li><b>file_info</b> - 获取文件/目录的元信息（大小、时间戳等）</li>
 *   <li><b>search_files</b> - 按 glob 模式搜索文件，支持深度控制</li>
 * </ul>
 * </p>
 * <p>
 * 安全特性：所有路径操作均经过校验，确保只能访问 {@link #allowedPaths} 中声明的目录范围，
 * 防止路径遍历攻击（如 {@code ../../../etc/passwd}）。
 * </p>
 */
@McpServer(name = "filesystem-server", version = "1.0.0")
public class FilesystemMcpServer {

    private static final Logger log = LoggerFactory.getLogger(FilesystemMcpServer.class);

    /** 文件系统操作的根目录，所有相对路径基于此目录解析 */
    private final Path rootPath;

    /** 允许访问的目录集合，用于路径安全校验 */
    private final Set<Path> allowedPaths;

    /**
     * 构造方法，使用单一根目录初始化文件系统服务器。
     * <p>允许访问的目录仅包含指定的根目录。</p>
     *
     * @param rootDir 根目录路径，支持相对路径和绝对路径
     */
    public FilesystemMcpServer(String rootDir) {
        this.rootPath = Paths.get(rootDir).toAbsolutePath().normalize();
        this.allowedPaths = new HashSet<>();
        this.allowedPaths.add(rootPath);
        log.info("Filesystem server initialized with root: {}", rootPath);
    }

    /**
     * 构造方法，使用根目录和额外允许目录初始化文件系统服务器。
     * <p>允许访问的目录包含根目录以及 {@code allowedDirs} 中指定的所有目录。</p>
     *
     * @param rootDir      根目录路径
     * @param allowedDirs  额外允许访问的目录列表
     */
    public FilesystemMcpServer(String rootDir, List<String> allowedDirs) {
        this.rootPath = Paths.get(rootDir).toAbsolutePath().normalize();
        this.allowedPaths = new HashSet<>();
        this.allowedPaths.add(rootPath);
        for (String dir : allowedDirs) {
            this.allowedPaths.add(Paths.get(dir).toAbsolutePath().normalize());
        }
        log.info("Filesystem server initialized with root: {} and allowed paths: {}", rootPath, allowedPaths);
    }

    /**
     * 读取文件内容。
     * <p>
     * 读取指定路径的文本文件并返回其全部内容。
     * 会检查文件是否存在且为常规文件（非目录）。
     * </p>
     *
     * @param path 文件路径，支持相对路径（相对于根目录）和绝对路径
     * @return 文件的文本内容；若文件不存在、非常规文件或无访问权限则返回错误信息
     */
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

    /**
     * 写入文件内容。
     * <p>
     * 将指定内容写入文件。若文件已存在则覆盖。
     * 可通过 {@code createDirs} 参数控制是否自动创建不存在的父目录。
     * </p>
     *
     * @param path       文件路径
     * @param content    要写入的文本内容
     * @param createDirs 是否自动创建父目录，可选；为 {@code true} 时若父目录不存在会自动创建
     * @return 操作结果描述；若无访问权限或写入失败则返回错误信息
     */
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

    /**
     * 列出目录中的文件和子目录。
     * <p>
     * 返回每个条目的名称、相对路径、类型（file/directory）和大小（仅文件）。
     * 支持可选的 glob 模式过滤。
     * </p>
     *
     * @param path    目录路径
     * @param pattern glob 过滤模式，可选（如 {@code "*.java"}）；为空时列出所有内容
     * @return 目录条目列表，以 JSON 数组格式返回，每个元素包含 name、path、type、size 字段
     */
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

    /**
     * 创建目录。
     * <p>
     * 递归创建目录及所有不存在的父目录（等效于 {@code mkdir -p}）。
     * 若目录已存在则返回成功，若路径已存在但不是目录则返回错误。
     * </p>
     *
     * @param path 要创建的目录路径
     * @return 操作结果描述
     */
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

    /**
     * 删除文件或目录。
     * <p>
     * 删除指定路径的文件或目录。对于目录，需通过 {@code recursive} 参数
     * 显式指定是否递归删除；若目录非空且未开启递归删除，将返回错误提示。
     * </p>
     *
     * @param path      要删除的文件或目录路径
     * @param recursive 是否递归删除目录，可选；为 {@code true} 时递归删除非空目录
     * @return 操作结果描述
     */
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

    /**
     * 移动或重命名文件/目录。
     * <p>
     * 将源路径移动到目标路径。若目标已存在则覆盖（使用 {@code REPLACE_EXISTING} 选项）。
     * 既可用于重命名，也可用于跨目录移动。
     * </p>
     *
     * @param source      源路径
     * @param destination 目标路径
     * @return 操作结果描述
     */
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

    /**
     * 复制文件或目录。
     * <p>
     * 对于目录，递归复制所有子文件和子目录。
     * 对于文件，直接复制到目标路径，若目标已存在则覆盖。
     * 若目标父目录不存在会自动创建。
     * </p>
     *
     * @param source      源路径
     * @param destination 目标路径
     * @return 操作结果描述
     */
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

    /**
     * 获取文件或目录的元信息。
     * <p>
     * 返回路径、绝对路径、类型（file/directory）、大小（字节）、
     * 创建时间、最后修改时间和最后访问时间。
     * </p>
     *
     * @param path 文件或目录路径
     * @return 文件/目录元信息，以 JSON 对象格式返回
     */
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

    /**
     * 按模式搜索文件。
     * <p>
     * 在指定目录下递归搜索文件名匹配给定 glob 模式的文件。
     * 支持控制最大搜索深度，防止在深层目录结构中耗时过长。
     * </p>
     *
     * @param directory 搜索起始目录
     * @param pattern   glob 匹配模式（如 {@code "*.java"}、{@code "*.txt"}）
     * @param maxDepth  最大递归深度，可选；默认为 10
     * @return 匹配的文件相对路径列表，以 JSON 数组格式返回
     */
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

    /**
     * 校验并解析路径。
     * <p>
     * 将传入的路径字符串解析为绝对规范化路径，并校验其是否在允许访问的目录范围内。
     * 相对路径会基于 {@link #rootPath} 进行解析。
     * </p>
     *
     * @param pathStr 路径字符串，支持相对路径和绝对路径
     * @return 校验通过的绝对规范化路径
     * @throws AccessDeniedException 当路径不在允许的目录范围内时抛出
     */
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
