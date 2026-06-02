package com.mcp.server.filesystem;

import com.mcp.protocol.ToolCallResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FilesystemMcpServer 单元测试
 */
class FilesystemMcpServerTest {

    @TempDir
    Path tempDir;

    @Test
    void testReadFile() throws IOException {
        // 创建测试文件
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello, World!");

        FilesystemMcpServer server = new FilesystemMcpServer(tempDir.toString());

        ToolCallResult result = server.readFile("test.txt");

        assertNotNull(result);
        assertFalse(result.getIsError());
        assertTrue(result.getContent().get(0).getText().contains("Hello, World!"));
    }

    @Test
    void testReadFileNotFound() {
        FilesystemMcpServer server = new FilesystemMcpServer(tempDir.toString());

        ToolCallResult result = server.readFile("nonexistent.txt");

        assertNotNull(result);
        assertTrue(result.getIsError());
        assertTrue(result.getContent().get(0).getText().contains("not found"));
    }

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

    @Test
    void testListDirectory() throws IOException {
        // 创建测试文件
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

    @Test
    void testDeleteFile() throws IOException {
        // 创建测试文件
        Path testFile = tempDir.resolve("to_delete.txt");
        Files.writeString(testFile, "delete me");

        FilesystemMcpServer server = new FilesystemMcpServer(tempDir.toString());

        ToolCallResult result = server.deleteFile("to_delete.txt", null);

        assertNotNull(result);
        assertFalse(result.getIsError());

        // 验证文件已删除
        assertFalse(Files.exists(testFile));
    }

    @Test
    void testMoveFile() throws IOException {
        // 创建测试文件
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

    @Test
    void testCopyFile() throws IOException {
        // 创建测试文件
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

    @Test
    void testFileInfo() throws IOException {
        // 创建测试文件
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

    @Test
    void testSearchFiles() throws IOException {
        // 创建测试文件
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

    @Test
    void testAllowedPaths() throws IOException {
        // 创建额外的允许目录
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
}
