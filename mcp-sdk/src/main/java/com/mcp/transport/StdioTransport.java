package com.mcp.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.protocol.JsonRpcMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Stdio 传输实现
 * 通过标准输入/输出进行通信
 */
public class StdioTransport implements Transport {

    private static final Logger log = LoggerFactory.getLogger(StdioTransport.class);

    private final ObjectMapper objectMapper;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final PrintStream outputWriter;

    private Consumer<JsonRpcMessage> messageHandler;
    private Consumer<Throwable> errorHandler;
    private Runnable closeHandler;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private Thread readerThread;

    public StdioTransport() {
        this(System.in, System.out);
    }

    public StdioTransport(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.outputWriter = new PrintStream(outputStream, true, StandardCharsets.UTF_8);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void start() throws Exception {
        if (connected.compareAndSet(false, true)) {
            log.info("Starting StdioTransport");
            startReaderThread();
        }
    }

    private void startReaderThread() {
        readerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                String line;
                while (connected.get() && (line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        log.debug("Received: {}", line);
                        JsonRpcMessage message = objectMapper.readValue(line, JsonRpcMessage.class);
                        if (messageHandler != null) {
                            messageHandler.accept(message);
                        }
                    } catch (Exception e) {
                        log.error("Failed to parse message: {}", line, e);
                        if (errorHandler != null) {
                            errorHandler.accept(e);
                        }
                    }
                }
            } catch (IOException e) {
                if (connected.get()) {
                    log.error("Error reading from stdin", e);
                    if (errorHandler != null) {
                        errorHandler.accept(e);
                    }
                }
            } finally {
                connected.set(false);
                if (closeHandler != null) {
                    closeHandler.run();
                }
            }
        }, "mcp-stdio-reader");

        readerThread.setDaemon(true);
        readerThread.start();
    }

    @Override
    public void send(JsonRpcMessage message) throws Exception {
        if (!connected.get()) {
            throw new IllegalStateException("Transport is not connected");
        }

        String json = objectMapper.writeValueAsString(message);
        log.debug("Sending: {}", json);

        synchronized (outputWriter) {
            outputWriter.println(json);
            outputWriter.flush();
        }
    }

    @Override
    public void onMessage(Consumer<JsonRpcMessage> handler) {
        this.messageHandler = handler;
    }

    @Override
    public void onError(Consumer<Throwable> handler) {
        this.errorHandler = handler;
    }

    @Override
    public void onClose(Runnable handler) {
        this.closeHandler = handler;
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public void close() throws IOException {
        if (connected.compareAndSet(true, false)) {
            log.info("Closing StdioTransport");

            if (readerThread != null) {
                readerThread.interrupt();
            }

            outputStream.close();
        }
    }
}
