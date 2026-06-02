package com.mcp.transport;

import com.mcp.protocol.JsonRpcMessage;

import java.io.Closeable;
import java.util.function.Consumer;

/**
 * 传输层接口
 */
public interface Transport extends Closeable {

    /**
     * 启动传输
     */
    void start() throws Exception;

    /**
     * 发送消息
     */
    void send(JsonRpcMessage message) throws Exception;

    /**
     * 设置消息接收回调
     */
    void onMessage(Consumer<JsonRpcMessage> handler);

    /**
     * 设置错误回调
     */
    void onError(Consumer<Throwable> handler);

    /**
     * 设置关闭回调
     */
    void onClose(Runnable handler);

    /**
     * 判断是否已连接
     */
    boolean isConnected();
}
