package com.mcp.server.redis;

import com.mcp.annotation.McpServer;
import com.mcp.annotation.McpTool;
import com.mcp.annotation.Param;
import com.mcp.protocol.ToolCallResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.*;

/**
 * Redis MCP Server 实现
 */
@McpServer(name = "redis-server", version = "1.0.0")
public class RedisMcpServer {

    private static final Logger log = LoggerFactory.getLogger(RedisMcpServer.class);

    private final JedisPool jedisPool;

    public RedisMcpServer(String host, int port, String password) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);

        if (password != null && !password.isEmpty()) {
            this.jedisPool = new JedisPool(poolConfig, host, port, 5000, password);
        } else {
            this.jedisPool = new JedisPool(poolConfig, host, port, 5000);
        }

        log.info("Redis connection pool initialized for {}:{}", host, port);
    }

    @McpTool(name = "get", description = "Get the value of a key")
    public ToolCallResult get(
            @Param(name = "key", description = "Redis key") String key
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(key);
            if (value == null) {
                return ToolCallResult.success("(nil)");
            }
            return ToolCallResult.success(value);
        } catch (Exception e) {
            return ToolCallResult.error("GET failed: " + e.getMessage());
        }
    }

    @McpTool(name = "set", description = "Set the value of a key")
    public ToolCallResult set(
            @Param(name = "key", description = "Redis key") String key,
            @Param(name = "value", description = "Value to set") String value,
            @Param(name = "ttl", description = "Expiration time in seconds", required = false) Long ttl
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (ttl != null) {
                jedis.setex(key, ttl, value);
            } else {
                jedis.set(key, value);
            }
            return ToolCallResult.success("OK");
        } catch (Exception e) {
            return ToolCallResult.error("SET failed: " + e.getMessage());
        }
    }

    @McpTool(name = "del", description = "Delete one or more keys")
    public ToolCallResult del(
            @Param(name = "keys", description = "Keys to delete (comma-separated)") String keys
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            String[] keyArray = keys.split(",");
            long deleted = jedis.del(keyArray);
            return ToolCallResult.success("Deleted " + deleted + " key(s)");
        } catch (Exception e) {
            return ToolCallResult.error("DEL failed: " + e.getMessage());
        }
    }

    @McpTool(name = "keys", description = "Find all keys matching a pattern")
    public ToolCallResult keys(
            @Param(name = "pattern", description = "Pattern to match (e.g., user:*)") String pattern
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys(pattern);
            return ToolCallResult.json(keys);
        } catch (Exception e) {
            return ToolCallResult.error("KEYS failed: " + e.getMessage());
        }
    }

    @McpTool(name = "type", description = "Determine the type stored at a key")
    public ToolCallResult type(
            @Param(name = "key", description = "Redis key") String key
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            String type = jedis.type(key);
            return ToolCallResult.success(type);
        } catch (Exception e) {
            return ToolCallResult.error("TYPE failed: " + e.getMessage());
        }
    }

    @McpTool(name = "ttl", description = "Get the time to live for a key")
    public ToolCallResult ttl(
            @Param(name = "key", description = "Redis key") String key
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            long ttl = jedis.ttl(key);
            if (ttl == -1) {
                return ToolCallResult.success("Key does not have an expiration");
            } else if (ttl == -2) {
                return ToolCallResult.success("Key does not exist");
            }
            return ToolCallResult.success(ttl + " seconds");
        } catch (Exception e) {
            return ToolCallResult.error("TTL failed: " + e.getMessage());
        }
    }

    @McpTool(name = "hget", description = "Get the value of a hash field")
    public ToolCallResult hget(
            @Param(name = "key", description = "Hash key") String key,
            @Param(name = "field", description = "Hash field") String field
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.hget(key, field);
            if (value == null) {
                return ToolCallResult.success("(nil)");
            }
            return ToolCallResult.success(value);
        } catch (Exception e) {
            return ToolCallResult.error("HGET failed: " + e.getMessage());
        }
    }

    @McpTool(name = "hset", description = "Set the value of a hash field")
    public ToolCallResult hset(
            @Param(name = "key", description = "Hash key") String key,
            @Param(name = "field", description = "Hash field") String field,
            @Param(name = "value", description = "Value to set") String value
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(key, field, value);
            return ToolCallResult.success("OK");
        } catch (Exception e) {
            return ToolCallResult.error("HSET failed: " + e.getMessage());
        }
    }

    @McpTool(name = "hgetall", description = "Get all fields and values in a hash")
    public ToolCallResult hgetall(
            @Param(name = "key", description = "Hash key") String key
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> entries = jedis.hgetAll(key);
            return ToolCallResult.json(entries);
        } catch (Exception e) {
            return ToolCallResult.error("HGETALL failed: " + e.getMessage());
        }
    }

    @McpTool(name = "lrange", description = "Get a range of elements from a list")
    public ToolCallResult lrange(
            @Param(name = "key", description = "List key") String key,
            @Param(name = "start", description = "Start index") long start,
            @Param(name = "stop", description = "Stop index") long stop
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            List<String> values = jedis.lrange(key, start, stop);
            return ToolCallResult.json(values);
        } catch (Exception e) {
            return ToolCallResult.error("LRANGE failed: " + e.getMessage());
        }
    }

    @McpTool(name = "llen", description = "Get the length of a list")
    public ToolCallResult llen(
            @Param(name = "key", description = "List key") String key
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            long length = jedis.llen(key);
            return ToolCallResult.success(String.valueOf(length));
        } catch (Exception e) {
            return ToolCallResult.error("LLEN failed: " + e.getMessage());
        }
    }

    @McpTool(name = "scard", description = "Get the number of members in a set")
    public ToolCallResult scard(
            @Param(name = "key", description = "Set key") String key
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            long count = jedis.scard(key);
            return ToolCallResult.success(String.valueOf(count));
        } catch (Exception e) {
            return ToolCallResult.error("SCARD failed: " + e.getMessage());
        }
    }

    @McpTool(name = "smembers", description = "Get all members in a set")
    public ToolCallResult smembers(
            @Param(name = "key", description = "Set key") String key
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> members = jedis.smembers(key);
            return ToolCallResult.json(members);
        } catch (Exception e) {
            return ToolCallResult.error("SMEMBERS failed: " + e.getMessage());
        }
    }

    @McpTool(name = "info", description = "Get server information")
    public ToolCallResult info(
            @Param(name = "section", description = "Section to get (optional)", required = false) String section
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            String info;
            if (section != null) {
                info = jedis.info(section);
            } else {
                info = jedis.info();
            }
            return ToolCallResult.success(info);
        } catch (Exception e) {
            return ToolCallResult.error("INFO failed: " + e.getMessage());
        }
    }

    @McpTool(name = "dbsize", description = "Get the number of keys in the current database")
    public ToolCallResult dbsize() {
        try (Jedis jedis = jedisPool.getResource()) {
            long size = jedis.dbSize();
            return ToolCallResult.success(String.valueOf(size));
        } catch (Exception e) {
            return ToolCallResult.error("DBSIZE failed: " + e.getMessage());
        }
    }

    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            log.info("Redis connection pool closed");
        }
    }
}
