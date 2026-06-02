package com.mcp.transport;

import com.mcp.protocol.JsonRpcMessage;

import java.io.Closeable;
import java.util.function.Consumer;

/**
 * MCP 传输层抽象接口。
 * <p>
 * 定义了 MCP（Model Context Protocol）通信的基础传输能力，包括消息的发送、
 * 接收回调的注册以及连接状态的管理。所有具体的传输实现（如 Stdio、HTTP SSE 等）
 * 都应实现此接口。
 * </p>
 * <p>
 * 该接口继承自 {@link Closeable}，支持通过 {@link #close()} 方法释放传输资源。
 * </p>
 */
public interface Transport extends Closeable {

    /**
     * 启动传输层，建立通信连接。
     * <p>
     * 调用此方法后，传输层开始监听并接收来自对端的消息。
     * 具体实现应确保该方法是幂等的，重复调用不应产生副作用。
     * </p>
     *
     * @throws Exception 启动过程中可能发生的异常
     */
    void start() throws Exception;

    /**
     * 通过当前传输层发送一条 JSON-RPC 消息。
     *
     * @param message 要发送的 JSON-RPC 消息，不能为 {@code null}
     * @throws Exception 发送过程中可能发生的异常（如连接已断开、序列化失败等）
     */
    void send(JsonRpcMessage message) throws Exception;

    /**
     * 注册消息接收回调处理器。
     * <p>
     * 当传输层从对端接收到一条完整的 JSON-RPC 消息时，将调用此回调。
     * 每次调用会覆盖之前设置的处理器。
     * </p>
     *
     * @param handler 消息处理器，接收 {@link JsonRpcMessage} 作为参数
     */
    void onMessage(Consumer<JsonRpcMessage> handler);

    /**
     * 注册错误回调处理器。
     * <p>
     * 当传输层发生异常（如网络错误、消息解析失败等）时，将调用此回调。
     * 每次调用会覆盖之前设置的处理器。
     * </p>
     *
     * @param handler 错误处理器，接收 {@link Throwable} 作为参数
     */
    void onError(Consumer<Throwable> handler);

    /**
     * 注册连接关闭回调处理器。
     * <p>
     * 当传输层连接关闭时（无论是正常关闭还是异常断开），将调用此回调。
     * 每次调用会覆盖之前设置的处理器。
     * </p>
     *
     * @param handler 关闭回调，无参数的 {@link Runnable}
     */
    void onClose(Runnable handler);

    /**
     * 判断当前传输层是否处于已连接状态。
     *
     * @return 如果已连接返回 {@code true}，否则返回 {@code false}
     */
    boolean isConnected();
}
