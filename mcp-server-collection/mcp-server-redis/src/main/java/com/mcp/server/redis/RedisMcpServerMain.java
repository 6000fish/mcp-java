package com.mcp.server.redis;

import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import com.mcp.transport.StdioTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis MCP Server 启动类。
 * <p>
 * 作为独立进程启动 Redis MCP Server，通过标准输入/输出（STDIO）传输方式
 * 与 MCP 客户端进行通信。
 * </p>
 * <p>
 * 启动时从环境变量读取 Redis 连接配置：
 * <ul>
 *   <li>{@code REDIS_HOST} - Redis 服务器地址，默认 localhost</li>
 *   <li>{@code REDIS_PORT} - Redis 服务器端口，默认 6379</li>
 *   <li>{@code REDIS_PASSWORD} - 认证密码，默认无密码</li>
 * </ul>
 * </p>
 */
public class RedisMcpServerMain {

    private static final Logger log = LoggerFactory.getLogger(RedisMcpServerMain.class);

    /**
     * 程序入口。
     * <p>
     * 初始化 Redis 连接、创建 MCP Server 实例、注册所有 Redis 工具、
     * 启动 STDIO 传输层，并注册 JVM 关闭钩子以确保连接池被正确释放。
     * </p>
     *
     * @param args 命令行参数（未使用）
     * @throws Exception 启动过程中可能抛出的异常
     */
    public static void main(String[] args) throws Exception {
        // 从环境变量读取配置
        String host = getEnvOrDefault("REDIS_HOST", "localhost");
        int port = Integer.parseInt(getEnvOrDefault("REDIS_PORT", "6379"));
        String password = getEnvOrDefault("REDIS_PASSWORD", null);

        log.info("Starting Redis MCP Server...");
        log.info("Connecting to Redis at {}:{}", host, port);

        // 创建 Redis Server
        RedisMcpServer redisServer = new RedisMcpServer(host, port, password);

        // 创建 MCP Server
        McpServer server = DefaultMcpServer.builder()
                .name("redis-server")
                .version("1.0.0")
                .build();

        // 注册工具
        server.tool("get", "Get the value of a key", params ->
                redisServer.get((String) params.get("key")));
        server.tool("set", "Set the value of a key", params ->
                redisServer.set(
                        (String) params.get("key"),
                        (String) params.get("value"),
                        params.get("ttl") != null ? Long.parseLong(String.valueOf(params.get("ttl"))) : null));
        server.tool("del", "Delete one or more keys", params ->
                redisServer.del((String) params.get("keys")));
        server.tool("keys", "Find all keys matching a pattern", params ->
                redisServer.keys((String) params.get("pattern")));
        server.tool("type", "Determine the type stored at a key", params ->
                redisServer.type((String) params.get("key")));
        server.tool("ttl", "Get the time to live for a key", params ->
                redisServer.ttl((String) params.get("key")));
        server.tool("hget", "Get the value of a hash field", params ->
                redisServer.hget(
                        (String) params.get("key"),
                        (String) params.get("field")));
        server.tool("hset", "Set the value of a hash field", params ->
                redisServer.hset(
                        (String) params.get("key"),
                        (String) params.get("field"),
                        (String) params.get("value")));
        server.tool("hgetall", "Get all fields and values in a hash", params ->
                redisServer.hgetall((String) params.get("key")));
        server.tool("lrange", "Get a range of elements from a list", params ->
                redisServer.lrange(
                        (String) params.get("key"),
                        Long.parseLong(String.valueOf(params.get("start"))),
                        Long.parseLong(String.valueOf(params.get("stop")))));
        server.tool("llen", "Get the length of a list", params ->
                redisServer.llen((String) params.get("key")));
        server.tool("scard", "Get the number of members in a set", params ->
                redisServer.scard((String) params.get("key")));
        server.tool("smembers", "Get all members in a set", params ->
                redisServer.smembers((String) params.get("key")));
        server.tool("info", "Get server information", params ->
                redisServer.info((String) params.get("section")));
        server.tool("dbsize", "Get the number of keys", params ->
                redisServer.dbsize());

        // 启动服务
        server.start(new StdioTransport());

        log.info("Redis MCP Server started. Waiting for connections...");

        // 保持运行
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down Redis MCP Server...");
            redisServer.close();
        }));

        // 阻塞主线程
        Thread.currentThread().join();
    }

    /**
     * 从环境变量中获取配置值，若不存在则返回默认值。
     *
     * @param name         环境变量名称
     * @param defaultValue 默认值
     * @return 环境变量的值，若未设置则返回 {@code defaultValue}
     */
    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }
}
