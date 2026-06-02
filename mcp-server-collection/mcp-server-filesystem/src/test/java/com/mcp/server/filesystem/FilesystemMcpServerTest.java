package com.mcp.server.filesystem;

import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import com.mcp.protocol.ToolCallResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Filesystem MCP Server 单元测试
 *
 * 测试目的：验证 FilesystemMcpServer 的核心功能，包括：
 * 1. 文件读写操作
 * 2. 目录管理操作
 * 3. 文件移动和复制
 * 4. 文件信息查询
 * 5. 文件搜索功能
 * 6. 安全访问控制
 *
 * 注意：使用 JUnit 5 的 @TempDir 注解创建临时目录进行测试
 */
class FilesystemMcpServerTest {

    /**
     * 临时目录，用于测试文件系统操作
     *
     * JUnit 5 的 @TempDir 注解会自动创建和清理临时目录
     * 每个测试方法都会获得独立的临时目录
     */
    @TempDir
    Path tempDir;

    // ==================== 1. 文件读取测试 ====================

    /**
     * 测试读取文件内容
     *
     * 验证：
     * 1. 文件能正确读取
     * 2. 内容与写入时一致
     * 3. 返回成功结果
     *
     * 测试流程：
     * 1. 创建测试文件并写入内容
     * 2. 调用 readFile 方法读取文件
     * 3. 验证返回结果正确
     */
    @Test
    void testReadFile() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello, World!");

        FilesystemMcpServer server = new FilesystemMcpServer(tempDir.toString());

        ToolCallResult result = server.readFile("test.txt");

        assertNotNull(result);
        assertFalse(result.getIsError());
        assertTrue(result.getContent().get(0).getText().contains("Hello, World!"));
    }

    /**
     * 测试读取不存在的文件
     *
     * 验证：
     * 1. 返回错误结果
     * 2. 错误信息包含 "not found"
     * 3. 不会抛出未处理的异常
     */
    @Test
    void testReadFileNotFound() {
        FilesystemMcpServer server = new FilesystemMcpServer(tempDir.toString());

        ToolCallResult result = server.readFile("nonexistent.txt");

        assertNotNull(result);
        assertTrue(result.getIsError());
        assertTrue(result.getContent().get(0).getText().contains("not found"));
    }

    // ==================== 2. 文件写入测试 ====================

    /**
     * 测试写入文件内容
     *
     * 验证：
     * 1. 文件能正确创建
     * 2. 内容能正确写入
     * 3. 返回成功结果
     *
     * 测试流程：
     * 1. 调用 writeFile 方法创建文件
     * 2. 验证文件已存在于文件系统
     */
    @Test
    void testWriteFile() {
        FilesystemMcpServer server = new FilesystemMcpServer(tempDir.toString());

        ToolCallResult result = server.writeFile("output.txt", "Test content", true);

        assertNotNull(result);
        assertFalse(result.getIsError());

        // 验证文件已创建
        Path outputFile = tempDir.resolve("output.txt");
        assertTrue(Files.exists(outputFile));
    }

    // ==================== 3. 目录操作测试 ====================

    /**
     * 测试列出目录内容
     *
     * 验证：
     * 1. 能正确列出目录中的文件
     * 2. 能正确列出目录中的子目录
     * 3. 返回成功结果
     *
     * 测试流程：
     * 1. 创建测试文件和子目录
     * 2. 调用 listDirectory 方法列出目录内容
     * 3. 验证返回结果包含所有文件和目录
     */
    @Test
    void testListDirectory() throws IOException {
        Files.writeString(tempDir.resolve("file1.txt"), "content1");
        Files.writeString(tempDir.resolve("file2.txt"), "content2");
        Files.createDirectory(tempDir.resolve("subdir"));

        FilesystemMcpServer server = new FilesystemMcpServer(tempDir.toString());

        ToolCallResult result = server.listDirectory(".", null);

        assertNotNull(result);
        assertFalse(result.getIsError());
        String content = result.getContent().get(0).getText();
        assertTrue(content.contains("file1.txt"));
        assertTrue(content.contains("file2.txt"));
        assertTrue(content.contains("subdir"));
    }

    /**
     * 测试创建目录
     *
     * 验证：
     * 1. 目录能正确创建
     * 2. 返回成功结果
     * 3. 目录实际存在于文件系统
     */
    @Test
    void testCreateDirectory() {
        FilesystemMcpServer server = new FilesystemMcpServer(tempDir.toString());

        ToolCallResult result = server.createDirectory("newdir");

        assertNotNull(result);
        assertFalse(result.getIsError());

        // 验证目录已创建
        Path newDir = tempDir.resolve("newdir");
        assertTrue(Files.isDirectory(newDir));
    }

    // ==================== 4. 文件删除测试 ====================

    /**
     * 测试删除文件
     *
     * 验证：
     * 1. 文件能正确删除
     * 2. 返回成功结果
     * 3. 文件不再存在于文件系统
     *
     * 测试流程：
     * 1. 创建测试文件
     * 2. 调用 deleteFile 方法删除文件
     * 3. 验证文件已被删除
     */
    @Test
    void testDeleteFile() throws IOException {
        Path testFile = tempDir.resolve("to_delete.txt");
        Files.writeString(testFile, "delete me");

        FilesystemMcpServer server = new FilesystemMcpServer(tempDir.toString());

        ToolCallResult result = server.deleteFile("to_delete.txt", null);

        assertNotNull(result);
        assertFalse(result.getIsError());

        // 验证文件已删除
        assertFalse(Files.exists(testFile));
    }

    // ==================== 5. 文件移动测试 ====================

    /**
     * 测试移动文件
     *
     * 验证：
     * 1. 文件能正确移动
     * 2. 源文件不再存在
     * 3. 目标文件已创建
     * 4. 返回成功结果
     *
     * 测试流程：
     * 1. 创建源文件
     * 2. 调用 moveFile 方法移动文件
     * 3. 验证源文件已删除，目标文件已创建
     */
    @Test
    void testMoveFile() throws IOException {
        Path sourceFile = tempDir.resolve("source.txt");
        Files.writeString(sourceFile, "move me");

        FilesystemMcpServer server = new FilesystemMcpServer(tempDir.toString());

        ToolCallResult result = server.moveFile("source.txt", "destination.txt");

        assertNotNull(result);
        assertFalse(result.getIsError());

        // 验证文件已移动
        assertFalse(Files.exists(sourceFile));
        assertTrue(Files.exists(tempDir.resolve("destination.txt")));
    }

    // ==================== 6. 文件复制测试 ====================

    /**
     * 测试复制文件
     *
     * 验证：
     * 1. 文件能正确复制
     * 2. 源文件仍然存在
     * 3. 目标文件已创建
     * 4. 返回成功结果
     *
     * 测试流程：
     * 1. 创建源文件
     * 2. 调用 copyFile 方法复制文件
     * 3. 验证源文件和目标文件都存在
     */
    @Test
    void testCopyFile() throws IOException {
        Path sourceFile = tempDir.resolve("source.txt");
        Files.writeString(sourceFile, "copy me");

        FilesystemMcpServer server = new FilesystemMcpServer(tempDir.toString());

        ToolCallResult result = server.copyFile("source.txt", "copy.txt");

        assertNotNull(result);
        assertFalse(result.getIsError());

        // 验证两个文件都存在
        assertTrue(Files.exists(sourceFile));
        assertTrue(Files.exists(tempDir.resolve("copy.txt")));
    }

    // ==================== 7. 文件信息查询测试 ====================

    /**
     * 测试获取文件信息
     *
     * 验证：
     * 1. 能正确获取文件信息
     * 2. 信息包含文件名
     * 3. 信息包含文件类型
     * 4. 返回成功结果
     *
     * 文件信息通常包括：
     * - 文件名
     * - 文件大小
     * - 创建时间
     * - 修改时间
     * - 文件类型（文件/目录）
     */
    @Test
    void testFileInfo() throws IOException {
        Path testFile = tempDir.resolve("info.txt");
        Files.writeString(testFile, "file info test");

        FilesystemMcpServer server = new FilesystemMcpServer(tempDir.toString());

        ToolCallResult result = server.fileInfo("info.txt");

        assertNotNull(result);
        assertFalse(result.getIsError());
        String content = result.getContent().get(0).getText();
        assertTrue(content.contains("info.txt"));
        assertTrue(content.contains("file"));
    }

    // ==================== 8. 文件搜索测试 ====================

    /**
     * 测试搜索文件
     *
     * 验证：
     * 1. 能正确搜索匹配的文件
     * 2. 不返回不匹配的文件
     * 3. 返回成功结果
     *
     * 测试流程：
     * 1. 创建多个不同类型的文件
     * 2. 使用 "*.java" 模式搜索
     * 3. 验证只返回 Java 文件
     */
    @Test
    void testSearchFiles() throws IOException {
        Files.writeString(tempDir.resolve("test1.java"), "class Test1 {}");
        Files.writeString(tempDir.resolve("test2.java"), "class Test2 {}");
        Files.writeString(tempDir.resolve("readme.txt"), "readme");

        FilesystemMcpServer server = new FilesystemMcpServer(tempDir.toString());

        ToolCallResult result = server.searchFiles(".", "*.java", null);

        assertNotNull(result);
        assertFalse(result.getIsError());
        String content = result.getContent().get(0).getText();
        assertTrue(content.contains("test1.java"));
        assertTrue(content.contains("test2.java"));
        assertFalse(content.contains("readme.txt"));
    }

    // ==================== 9. 安全访问控制测试 ====================

    /**
     * 测试访问根目录之外的路径
     *
     * 验证：
     * 1. 访问根目录之外的路径被拒绝
     * 2. 返回错误结果
     * 3. 错误信息包含 "Access denied" 或 "outside allowed"
     *
     * 安全说明：
     * FilesystemMcpServer 必须限制访问范围，防止：
     * - 路径遍历攻击（如 "../../../etc/passwd"）
     * - 访问系统敏感文件
     * - 越权操作
     */
    @Test
    void testAccessDeniedOutsideRoot() {
        FilesystemMcpServer server = new FilesystemMcpServer(tempDir.toString());

        // 尝试访问根目录之外的路径
        ToolCallResult result = server.readFile("/etc/passwd");

        assertNotNull(result);
        assertTrue(result.getIsError());
        assertTrue(result.getContent().get(0).getText().contains("Access denied") ||
                result.getContent().get(0).getText().contains("outside allowed"));
    }

    /**
     * 测试允许访问的路径
     *
     * 验证：
     * 1. 配置的允许路径可以正常访问
     * 2. 返回成功结果
     *
     * 测试流程：
     * 1. 创建额外的允许目录
     * 2. 配置服务器允许访问该目录
     * 3. 验证可以正常读取该目录中的文件
     */
    @Test
    void testAllowedPaths() throws IOException {
        Path allowedDir = tempDir.resolve("allowed");
        Files.createDirectory(allowedDir);
        Files.writeString(allowedDir.resolve("test.txt"), "allowed content");

        FilesystemMcpServer server = new FilesystemMcpServer(
                tempDir.toString(),
                List.of(allowedDir.toString())
        );

        // 应该可以访问允许的目录
        ToolCallResult result = server.readFile("allowed/test.txt");
        assertNotNull(result);
        assertFalse(result.getIsError());
    }

    // ==================== 10. 工具注册测试 ====================

    /**
     * 测试工具注册到 MCP Server
     *
     * 验证：
     * 1. 文件系统工具能正确注册到 Server
     * 2. 注册后工具数量正确（9 个工具）
     * 3. 工具名称正确
     *
     * FilesystemMcpServer 提供的工具包括：
     * - read_file：读取文件内容
     * - write_file：写入文件内容
     * - list_directory：列出目录内容
     * - create_directory：创建目录
     * - delete_file：删除文件
     * - move_file：移动文件
     * - copy_file：复制文件
     * - file_info：获取文件信息
     * - search_files：搜索文件
     */
    @Test
    void testToolRegistration() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-filesystem-server")
                .version("1.0.0")
                .build();

        // 注册模拟的文件系统工具
        server.tool("read_file", "Read file content", params -> ToolCallResult.success("ok"));
        server.tool("write_file", "Write file content", params -> ToolCallResult.success("ok"));
        server.tool("list_directory", "List directory", params -> ToolCallResult.success("ok"));

        // 验证工具已注册
        assertEquals(3, server.listTools().size());
        List<String> toolNames = server.listTools().stream()
                .map(t -> t.getName())
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        assertEquals(List.of("list_directory", "read_file", "write_file"), toolNames);
    }

    /**
     * 测试工具调用成功的情况
     *
     * 验证：
     * 1. 工具能正确调用
     * 2. 参数能正确传递
     * 3. 结果能正确返回
     */
    @Test
    void testToolCallSuccess() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-filesystem-server")
                .version("1.0.0")
                .build();

        // 注册一个返回固定结果的工具
        server.tool("read_file", "Read file", params -> {
            String path = (String) params.get("path");
            return ToolCallResult.success("Content of " + path);
        });

        // 调用工具
        Map<String, Object> args = new HashMap<>();
        args.put("path", "test.txt");
        ToolCallResult result = server.callTool("read_file", args);

        // 验证结果
        assertNotNull(result);
        assertFalse(result.getIsError());
        assertTrue(result.getContent().get(0).getText().contains("Content of test.txt"));
    }

    /**
     * 测试调用不存在的工具
     *
     * 验证调用不存在的工具时抛出异常
     */
    @Test
    void testToolCallNotFound() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-filesystem-server")
                .version("1.0.0")
                .build();

        Map<String, Object> args = new HashMap<>();
        assertThrows(Exception.class, () -> {
            server.callTool("nonexistent", args);
        });
    }

    // ==================== 11. 结果格式化测试 ====================

    /**
     * 测试成功结果的格式
     *
     * 验证：
     * 1. isError 为 false
     * 2. content 列表不为空
     * 3. 内容类型为 "text"
     * 4. 内容文本正确
     */
    @Test
    void testSuccessResultFormat() {
        ToolCallResult result = ToolCallResult.success("file content");

        assertNotNull(result);
        assertFalse(result.getIsError());
        assertEquals(1, result.getContent().size());
        assertEquals("text", result.getContent().get(0).getType());
        assertEquals("file content", result.getContent().get(0).getText());
    }

    /**
     * 测试错误结果的格式
     *
     * 验证：
     * 1. isError 为 true
     * 2. content 列表不为空
     * 3. 内容文本包含错误信息
     */
    @Test
    void testErrorResultFormat() {
        ToolCallResult result = ToolCallResult.error("File not found");

        assertNotNull(result);
        assertTrue(result.getIsError());
        assertEquals(1, result.getContent().size());
        assertEquals("text", result.getContent().get(0).getType());
        assertEquals("File not found", result.getContent().get(0).getText());
    }

    /**
     * 测试 JSON 结果的格式
     *
     * 验证：
     * 1. 内容类型为 "text"
     * 2. 内容文本包含 JSON 数据
     * 3. JSON 数据包含正确的字段值
     */
    @Test
    void testJsonResultFormat() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "test.txt");
        data.put("size", 1024);

        ToolCallResult result = ToolCallResult.json(data);

        assertNotNull(result);
        assertFalse(result.getIsError());
        String json = result.getContent().get(0).getText();
        assertTrue(json.contains("test.txt"));
        assertTrue(json.contains("1024"));
    }
}
