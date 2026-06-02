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
 * 基于标准输入/输出（Stdio）的 MCP 传输层实现。
 * <p>
 * 该实现通过进程的标准输入（stdin）接收 JSON-RPC 消息，通过标准输出（stdout）发送消息，
 * 适用于本地进程间通信场景，例如 MCP 客户端以子进程方式启动 MCP 服务器时的通信。
 * </p>
 * <p>
 * 消息格式为每行一条 JSON 字符串（JSON Lines 协议）。内部通过独立的守护线程异步读取
 * 输入流中的消息，并通过回调机制通知上层。
 * </p>
 *
 * @see Transport
 */
public class StdioTransport implements Transport {

    /** 日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(StdioTransport.class);

    /** JSON 序列化/反序列化器 */
    private final ObjectMapper objectMapper;

    /** 输入流，通常为 {@link System#in} */
    private final InputStream inputStream;

    /** 输出流，通常为 {@link System#out} */
    private final OutputStream outputStream;

    /** 基于输出流的打印流，用于按行写入 JSON 消息 */
    private final PrintStream outputWriter;

    /** 消息接收回调处理器 */
    private Consumer<JsonRpcMessage> messageHandler;

    /** 错误回调处理器 */
    private Consumer<Throwable> errorHandler;

    /** 连接关闭回调处理器 */
    private Runnable closeHandler;

    /** 连接状态标志，使用原子操作保证线程安全 */
    private final AtomicBoolean connected = new AtomicBoolean(false);

    /** 后台消息读取线程 */
    private Thread readerThread;

    /**
     * 使用系统默认的标准输入/输出创建 Stdio 传输实例。
     * <p>
     * 等价于 {@code new StdioTransport(System.in, System.out)}。
     * </p>
     */
    public StdioTransport() {
        this(System.in, System.out);
    }

    /**
     * 使用指定的输入流和输出流创建 Stdio 传输实例。
     *
     * @param inputStream  输入流，用于接收 JSON-RPC 消息
     * @param outputStream 输出流，用于发送 JSON-RPC 消息
     */
    public StdioTransport(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.outputWriter = new PrintStream(outputStream, true, StandardCharsets.UTF_8);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 启动 Stdio 传输层，开始从输入流中异步读取消息。
     * <p>
     * 该方法是幂等的，如果传输层已经启动则不会重复启动。
     * </p>
     *
     * @throws Exception 启动过程中可能发生的异常
     */
    @Override
    public void start() throws Exception {
        if (connected.compareAndSet(false, true)) {
            log.info("Starting StdioTransport");
            startReaderThread();
        }
    }

    /**
     * 启动后台守护线程，持续从输入流中按行读取 JSON 消息并分发给消息处理器。
     * <p>
     * 读取到的每一行文本会被反序列化为 {@link JsonRpcMessage} 对象。
     * 空行将被忽略。解析失败时会通过错误处理器通知上层。
     * 当输入流结束或连接关闭时，线程退出并触发关闭回调。
     * </p>
     */
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

    /**
     * 将 JSON-RPC 消息序列化为 JSON 字符串并写入输出流。
     * <p>
     * 写入操作是线程安全的，使用 {@code synchronized} 保证多线程并发发送时不会交错。
     * 每条消息占一行，以换行符结尾（JSON Lines 协议）。
     * </p>
     *
     * @param message 要发送的 JSON-RPC 消息，不能为 {@code null}
     * @throws IllegalStateException 如果传输层未处于已连接状态
     * @throws Exception 序列化或写入过程中可能发生的异常
     */
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

    /**
     * 注册消息接收回调处理器。
     *
     * @param handler 消息处理器，每次接收到消息时被调用
     */
    @Override
    public void onMessage(Consumer<JsonRpcMessage> handler) {
        this.messageHandler = handler;
    }

    /**
     * 注册错误回调处理器。
     *
     * @param handler 错误处理器，当发生异常时被调用
     */
    @Override
    public void onError(Consumer<Throwable> handler) {
        this.errorHandler = handler;
    }

    /**
     * 注册连接关闭回调处理器。
     *
     * @param handler 关闭回调，当传输层关闭时被调用
     */
    @Override
    public void onClose(Runnable handler) {
        this.closeHandler = handler;
    }

    /**
     * 判断当前传输层是否处于已连接状态。
     *
     * @return 已连接返回 {@code true}，否则返回 {@code false}
     */
    @Override
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * 关闭传输层，释放所有资源。
     * <p>
     * 会中断后台读取线程并关闭输出流。该方法是幂等的，重复调用安全。
     * </p>
     *
     * @throws IOException 关闭输出流时可能发生的异常
     */
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
