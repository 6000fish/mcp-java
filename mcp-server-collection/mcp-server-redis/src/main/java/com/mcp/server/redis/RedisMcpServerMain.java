package com.mcp.server.redis;

import com.mcp.server.DefaultMcpServer;
import com.mcp.server.McpServer;
import com.mcp.transport.StdioTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis MCP Server 启动类
 */
public class RedisMcpServerMain {

    private static final Logger log = LoggerFactory.getLogger(RedisMcpServerMain.class);

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

    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }
}
