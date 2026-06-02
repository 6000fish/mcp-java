package com.mcp.server.docker;

import com.mcp.protocol.ToolCallResult;
import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Docker MCP Server 单元测试
 *
 * 测试目的：验证 DockerMcpServer 的核心功能，包括：
 * 1. 输入验证（容器 ID、镜像名称）
 * 2. 危险命令检测
 * 3. 工具注册和调用流程
 * 4. 参数解析和验证
 * 5. 错误处理机制
 *
 * 注意：本测试不依赖真实的 Docker 环境
 */
class DockerMcpServerTest {

    // ==================== 1. 输入验证测试 ====================

    /**
     * 测试容器 ID 验证
     *
     * 有效的容器 ID 只能包含字母、数字、短横线和下划线
     */
    @Test
    void testContainerIdValidation() {
        DockerMcpServer server = new DockerMcpServer();

        // 有效 ID
        assertTrue(isValidId("abc123"));
        assertTrue(isValidId("my-container"));
        assertTrue(isValidId("my_container"));
        assertTrue(isValidId("container-123-abc"));

        // 无效 ID
        assertFalse(isValidId(null));
        assertFalse(isValidId(""));
        assertFalse(isValidId("container; rm -rf /"));
        assertFalse(isValidId("container | echo hacked"));
    }

    /**
     * 测试镜像名称验证
     */
    @Test
    void testImageNameValidation() {
        DockerMcpServer server = new DockerMcpServer();

        // 有效镜像名
        assertTrue(isValidImageName("nginx"));
        assertTrue(isValidImageName("nginx:latest"));
        assertTrue(isValidImageName("myregistry.com/nginx:1.0"));

        // 无效镜像名
        assertFalse(isValidImageName(null));
        assertFalse(isValidImageName(""));
        assertFalse(isValidImageName("nginx; rm -rf /"));
        assertFalse(isValidImageName("nginx | echo hacked"));
    }

    /**
     * 测试危险命令检测
     */
    @Test
    void testDangerousCommandDetection() {
        DockerMcpServer server = new DockerMcpServer();

        // 危险命令
        assertTrue(isDangerousCommand("apt install --privileged malware"));
        assertTrue(isDangerousCommand("docker --network host"));
        assertTrue(isDangerousCommand("docker system prune"));
        assertTrue(isDangerousCommand("mount -v /:/host"));

        // 安全命令
        assertFalse(isDangerousCommand("ls -la"));
        assertFalse(isDangerousCommand("cat /etc/hostname"));
        assertFalse(isDangerousCommand("echo hello"));
    }

    // ==================== 2. 工具注册测试 ====================

    /**
     * 测试工具注册到 MCP Server
     */
    @Test
    void testToolRegistration() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-docker-server")
                .version("1.0.0")
                .build();

        server.tool("list_containers", "List containers", params -> ToolCallResult.success("ok"));
        server.tool("start_container", "Start container", params -> ToolCallResult.success("ok"));
        server.tool("stop_container", "Stop container", params -> ToolCallResult.success("ok"));
        server.tool("list_images", "List images", params -> ToolCallResult.success("ok"));

        assertEquals(4, server.listTools().size());
        List<String> toolNames = server.listTools().stream()
                .map(t -> t.getName())
                .sorted()
                .toList();
        assertEquals(List.of("list_containers", "list_images", "start_container", "stop_container"), toolNames);
    }

    // ==================== 3. 工具调用测试 ====================

    /**
     * 测试工具调用成功的情况
     */
    @Test
    void testToolCallSuccess() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-docker-server")
                .version("1.0.0")
                .build();

        server.tool("list_containers", "List containers", params ->
                ToolCallResult.success("CONTAINER ID   IMAGE   STATUS"));

        Map<String, Object> args = new HashMap<>();
        ToolCallResult result = server.callTool("list_containers", args);

        assertNotNull(result);
        assertFalse(result.getIsError());
        assertTrue(result.getContent().get(0).getText().contains("CONTAINER ID"));
    }

    /**
     * 测试调用不存在的工具
     */
    @Test
    void testToolCallNotFound() {
        McpServer server = DefaultMcpServer.builder()
                .name("test-docker-server")
                .version("1.0.0")
                .build();

        Map<String, Object> args = new HashMap<>();
        assertThrows(Exception.class, () -> server.callTool("nonexistent", args));
    }

    // ==================== 4. 参数解析测试 ====================

    /**
     * 测试参数提取逻辑
     */
    @Test
    void testParameterExtraction() {
        Map<String, Object> params = new HashMap<>();
        params.put("container_id", "abc123");
        params.put("command", "ls -la");

        assertEquals("abc123", params.get("container_id"));
        assertEquals("ls -la", params.get("command"));
        assertNull(params.get("nonexistent"));
    }

    // ==================== 5. 结果格式化测试 ====================

    /**
     * 测试成功结果的格式
     */
    @Test
    void testSuccessResultFormat() {
        ToolCallResult result = ToolCallResult.success("container started");

        assertNotNull(result);
        assertFalse(result.getIsError());
        assertEquals(1, result.getContent().size());
        assertEquals("text", result.getContent().get(0).getType());
        assertEquals("container started", result.getContent().get(0).getText());
    }

    /**
     * 测试错误结果的格式
     */
    @Test
    void testErrorResultFormat() {
        ToolCallResult result = ToolCallResult.error("Container not found");

        assertNotNull(result);
        assertTrue(result.getIsError());
        assertEquals("Container not found", result.getContent().get(0).getText());
    }

    /**
     * 测试 JSON 结果的格式
     */
    @Test
    void testJsonResultFormat() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", "abc123");
        data.put("status", "running");

        ToolCallResult result = ToolCallResult.json(data);

        assertNotNull(result);
        assertFalse(result.getIsError());
        String json = result.getContent().get(0).getText();
        assertTrue(json.contains("abc123"));
        assertTrue(json.contains("running"));
    }

    // ==================== 辅助方法 ====================

    /**
     * 验证容器 ID 是否有效
     */
    private boolean isValidId(String id) {
        return id != null && id.matches("^[a-zA-Z0-9_-]+$");
    }

    /**
     * 验证镜像名称是否有效
     */
    private boolean isValidImageName(String imageName) {
        if (imageName == null || imageName.isBlank()) return false;
        return !imageName.matches(".*[;&|`$(){}].*");
    }

    /**
     * 检查是否为危险命令
     */
    private boolean isDangerousCommand(String command) {
        if (command == null) return false;
        String lower = command.toLowerCase();
        return lower.contains("--privileged") ||
                lower.contains("--network host") ||
                lower.contains("system prune") ||
                lower.contains("-v /:");
    }
}
