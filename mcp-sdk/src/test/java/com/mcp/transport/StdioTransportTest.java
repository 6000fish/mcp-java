package com.mcp.transport;

import com.mcp.protocol.JsonRpcMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stdio 传输层单元测试
 */
class StdioTransportTest {

    private StdioTransport transport;
    private PipedOutputStream inputWriter;

    @AfterEach
    void tearDown() throws Exception {
        if (transport != null) {
            transport.close();
        }
        if (inputWriter != null) {
            inputWriter.close();
        }
    }

    @Test
    void testStartAndClose() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        createTransport(output);

        assertFalse(transport.isConnected());

        transport.start();
        assertTrue(transport.isConnected());

        transport.close();
        assertFalse(transport.isConnected());
    }

    @Test
    void testStartIdempotent() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        createTransport(output);

        transport.start();
        transport.start();

        assertTrue(transport.isConnected());
    }

    @Test
    void testCloseIdempotent() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        createTransport(output);

        transport.start();
        transport.close();
        transport.close();

        assertFalse(transport.isConnected());
    }

    @Test
    void testSendWritesJsonLine() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        createTransport(output);
        transport.start();

        transport.send(JsonRpcMessage.request(1, "ping", null));

        String json = output.toString(StandardCharsets.UTF_8).trim();
        assertTrue(json.contains("\"jsonrpc\":\"2.0\""));
        assertTrue(json.contains("\"id\":1"));
        assertTrue(json.contains("\"method\":\"ping\""));
    }

    @Test
    void testReceiveMessage() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        createTransport(output);

        CompletableFuture<JsonRpcMessage> received = new CompletableFuture<>();
        transport.onMessage(received::complete);
        transport.start();

        writeInputLine("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}");

        JsonRpcMessage message = received.get(2, TimeUnit.SECONDS);
        assertNotNull(message);
        assertEquals("ping", message.getMethod());
        assertEquals(1, message.getId());
        assertTrue(message.isRequest());
    }

    @Test
    void testInvalidJsonTriggersErrorHandler() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        createTransport(output);

        CompletableFuture<Throwable> error = new CompletableFuture<>();
        transport.onError(error::complete);
        transport.start();

        writeInputLine("not-json");

        Throwable ex = error.get(2, TimeUnit.SECONDS);
        assertNotNull(ex);
    }

    @Test
    void testCloseHandlerCalled() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        createTransport(output);

        CompletableFuture<Void> closed = new CompletableFuture<>();
        transport.onClose(() -> closed.complete(null));
        transport.start();

        inputWriter.close();

        assertDoesNotThrow(() -> closed.get(2, TimeUnit.SECONDS));
        assertFalse(transport.isConnected());
    }

    @Test
    void testSendWhenNotConnected() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        createTransport(output);

        JsonRpcMessage message = JsonRpcMessage.notification("test", null);
        assertThrows(IllegalStateException.class, () -> transport.send(message));
    }

    private void createTransport(ByteArrayOutputStream output) {
        try {
            PipedInputStream input = new PipedInputStream();
            inputWriter = new PipedOutputStream(input);
            transport = new StdioTransport(input, output);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writeInputLine(String line) {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(inputWriter, StandardCharsets.UTF_8), true);
        writer.println(line);
    }
}
