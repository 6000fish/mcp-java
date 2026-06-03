package com.mcp.transport;

import com.mcp.client.DefaultMcpClient;
import com.mcp.client.McpClient;
import com.mcp.exception.McpException;
import com.mcp.protocol.Implementation;
import com.mcp.protocol.InitializeRequest;
import com.mcp.protocol.InitializeResult;
import com.mcp.protocol.Prompt;
import com.mcp.protocol.PromptResult;
import com.mcp.protocol.Resource;
import com.mcp.protocol.ResourceContent;
import com.mcp.protocol.Tool;
import com.mcp.protocol.ToolCallResult;
import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StdioTransportE2ETest {

    private McpServer server;
    private McpClient client;
    private StdioTransport serverTransport;
    private StdioTransport clientTransport;
    private PipedInputStream serverInput;
    private PipedOutputStream clientOutput;
    private PipedInputStream clientInput;
    private PipedOutputStream serverOutput;

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            client.disconnect();
        }
        if (server != null) {
            server.stop();
        }
        closeQuietly(clientOutput);
        closeQuietly(serverOutput);
        closeQuietly(clientInput);
        closeQuietly(serverInput);
    }

    @Test
    void testInitializeOverStdioTransport() {
        assertTimeout(Duration.ofSeconds(5), () -> {
            setUpConnectedClientAndServer();

            InitializeResult result = client.initialize(createInitializeRequest());

            assertNotNull(result);
            assertEquals("2024-11-05", result.getProtocolVersion());
            assertEquals("stdio-e2e-server", result.getServerInfo().getName());
            assertEquals("1.0.0", result.getServerInfo().getVersion());
            assertNotNull(result.getCapabilities());
            assertNotNull(result.getCapabilities().getTools());
            assertNotNull(result.getCapabilities().getResources());
            assertNotNull(result.getCapabilities().getPrompts());
        });
    }

    @Test
    void testServerCapabilitiesOverStdioTransport() {
        assertTimeout(Duration.ofSeconds(5), () -> {
            setUpConnectedClientAndServer();
            client.initialize(createInitializeRequest());

            List<Tool> tools = client.listTools();
            assertEquals(1, tools.size());
            assertEquals("greet", tools.get(0).getName());
            assertEquals("打招呼", tools.get(0).getDescription());

            ToolCallResult toolResult = client.callTool("greet", Map.of("name", "MCP"));
            assertFalse(toolResult.getIsError());
            assertEquals("Hello, MCP", toolResult.getContent().get(0).getText());

            List<Resource> resources = client.listResources();
            assertEquals(1, resources.size());
            assertEquals("config://app", resources.get(0).getUri());
            assertEquals("app-config", resources.get(0).getName());

            List<ResourceContent> contents = client.readResource("config://app");
            assertEquals(1, contents.size());
            assertEquals("config://app", contents.get(0).getUri());
            assertEquals("config-content", contents.get(0).getText());

            List<Prompt> prompts = client.listPrompts();
            assertEquals(1, prompts.size());
            assertEquals("review", prompts.get(0).getName());

            PromptResult prompt = client.getPrompt("review", Map.of("language", "Java"));
            assertEquals("Review Java", prompt.getDescription());
            assertEquals("user", prompt.getMessages().get(0).getRole());
            assertEquals("Please review Java", prompt.getMessages().get(0).getContent().getText());

            assertTrue(client.ping());
        });
    }

    @Test
    void testServerErrorResponseOverStdioTransport() {
        assertTimeout(Duration.ofSeconds(5), () -> {
            setUpConnectedClientAndServer();
            client.initialize(createInitializeRequest());

            McpException ex = assertThrows(McpException.class, () -> client.callTool("missing", Map.of()));

            assertEquals(-32601, ex.getCode());
            assertTrue(ex.getMessage().contains("missing"));
        });
    }

    private void setUpConnectedClientAndServer() throws Exception {
        server = DefaultMcpServer.builder()
                .name("stdio-e2e-server")
                .version("1.0.0")
                .build();

        server.tool("greet", "打招呼", arguments -> {
            String name = (String) arguments.getOrDefault("name", "World");
            return ToolCallResult.success("Hello, " + name);
        });
        server.resource("config://app", "app-config", "应用配置", uri -> ResourceContent.text(uri, "config-content"));
        server.prompt("review", "代码审查", arguments -> {
            String language = arguments.getOrDefault("language", "Java");
            return PromptResult.builder()
                    .description("Review " + language)
                    .messages(List.of(PromptResult.Message.builder()
                            .role("user")
                            .content(PromptResult.Content.text("Please review " + language))
                            .build()))
                    .build();
        });

        clientOutput = new PipedOutputStream();
        serverInput = new PipedInputStream(clientOutput);
        serverOutput = new PipedOutputStream();
        clientInput = new PipedInputStream(serverOutput);

        serverTransport = new StdioTransport(serverInput, serverOutput);
        clientTransport = new StdioTransport(clientInput, clientOutput);

        server.start(serverTransport);
        client = DefaultMcpClient.builder()
                .name("stdio-e2e-client")
                .version("1.0.0")
                .timeout(2)
                .build();
        client.connect(clientTransport);
    }

    private InitializeRequest createInitializeRequest() {
        return InitializeRequest.builder()
                .protocolVersion("2024-11-05")
                .clientInfo(Implementation.builder()
                        .name("stdio-e2e-client")
                        .version("1.0.0")
                        .build())
                .build();
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}
