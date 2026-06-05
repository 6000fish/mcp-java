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
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.*;

/**
 * Redis MCP Server 实现。
 * <p>
 * 本类通过 MCP（Model Context Protocol）协议将 Redis 数据库操作暴露为可调用的工具，
 * 使 AI 模型能够直接与 Redis 进行交互。
 * </p>
 * <p>
 * 提供以下 MCP 工具：
 * <ul>
 *   <li><b>get</b> - 获取指定键的值</li>
 *   <li><b>set</b> - 设置键值对，支持可选的过期时间</li>
 *   <li><b>del</b> - 默认安全策略下禁用删除操作</li>
 *   <li><b>keys</b> - 按模式匹配查找键</li>
 *   <li><b>type</b> - 获取键存储的数据类型</li>
 *   <li><b>ttl</b> - 获取键的剩余生存时间</li>
 *   <li><b>hget</b> - 获取哈希表中指定字段的值</li>
 *   <li><b>hset</b> - 设置哈希表中指定字段的值</li>
 *   <li><b>hgetall</b> - 获取哈希表中所有字段和值</li>
 *   <li><b>lrange</b> - 获取列表中指定范围的元素</li>
 *   <li><b>llen</b> - 获取列表长度</li>
 *   <li><b>scard</b> - 获取集合中的成员数量</li>
 *   <li><b>smembers</b> - 获取集合中的所有成员</li>
 *   <li><b>info</b> - 获取 Redis 服务器信息</li>
 *   <li><b>dbsize</b> - 获取当前数据库中的键总数</li>
 * </ul>
 * </p>
 * <p>
 * 使用 Jedis 连接池管理 Redis 连接，支持带密码和无密码两种认证方式。
 * </p>
 */
@McpServer(name = "redis-server", version = "1.0.0")
public class RedisMcpServer {

    private static final Logger log = LoggerFactory.getLogger(RedisMcpServer.class);

    private static final int MAX_KEY_LENGTH = 512;
    private static final int MAX_FIELD_LENGTH = 512;
    private static final int MAX_KEYS_RETURNED = 100;
    private static final int MAX_COLLECTION_ITEMS = 100;
    private static final long MAX_TTL_SECONDS = 30L * 24 * 60 * 60;
    private static final Set<String> SAFE_INFO_SECTIONS = Set.of("server", "clients", "memory", "stats", "keyspace", "cpu");

    /** Jedis 连接池，管理与 Redis 服务器的连接 */
    private final JedisPool jedisPool;

    /**
     * 构造方法，初始化 Redis 连接池。
     * <p>
     * 使用 JedisPool 连接池，配置最大连接数为 10，最大空闲连接数为 5，
     * 最小空闲连接数为 1，连接超时 5 秒。
     * </p>
     *
     * @param host     Redis 服务器地址
     * @param port     Redis 服务器端口
     * @param password Redis 认证密码，为 {@code null} 或空字符串时表示无密码
     */
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

    /**
     * 获取指定键的值。
     * <p>等效于 Redis 命令 {@code GET key}。若键不存在，返回 {@code "(nil)"}。</p>
     *
     * @param key 要查询的 Redis 键名
     * @return 键对应的字符串值，或 {@code "(nil)"}（键不存在时），或错误信息
     */
    @McpTool(name = "get", description = "Get the string value stored at a Redis key. Use this for simple string keys; use hget or hgetall for hash fields.")
    public ToolCallResult get(
            @Param(name = "key", description = "Redis key to read") String key
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            requireSafeKey(key);
            String value = jedis.get(key);
            if (value == null) {
                return ToolCallResult.success("(nil)");
            }
            return ToolCallResult.success(value);
        } catch (Exception e) {
            return ToolCallResult.error("GET failed: " + e.getMessage());
        }
    }

    /**
     * 设置键值对。
     * <p>
     * 等效于 Redis 命令 {@code SET key value} 或 {@code SETEX key ttl value}。
     * 若指定 {@code ttl} 参数，键将在过期时间后自动删除。
     * </p>
     *
     * @param key   要设置的 Redis 键名
     * @param value 要存储的值
     * @param ttl   过期时间（秒），可选；为 {@code null} 时表示不设置过期时间
     * @return 操作结果，成功时返回 {@code "OK"}，失败时返回错误信息
     */
    @McpTool(name = "set", description = "Set a Redis string key to a value, optionally with a TTL in seconds. Use this for string values; use hset for hash fields.")
    public ToolCallResult set(
            @Param(name = "key", description = "Redis key to write") String key,
            @Param(name = "value", description = "String value to store") String value,
            @Param(name = "ttl", description = "Optional expiration time in seconds. Leave empty to keep the key persistent.", required = false) Long ttl
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            requireSafeKey(key);
            if (ttl != null) {
                requireSafeTtl(ttl);
                jedis.setex(key, ttl, value);
            } else {
                jedis.set(key, value);
            }
            return ToolCallResult.success("OK");
        } catch (Exception e) {
            return ToolCallResult.error("SET failed: " + e.getMessage());
        }
    }

    /**
     * 删除一个或多个键。
     * <p>
     * 等效于 Redis 命令 {@code DEL key1 key2 ...}。
     * 多个键名以逗号分隔，方法内部会自动拆分。
     * </p>
     *
     * @param keys 要删除的键名，多个键以逗号（{@code ,}）分隔
     * @return 操作结果，包含实际删除的键数量
     */
    @McpTool(name = "del", description = "Redis key deletion is disabled by the default MCP safety policy to avoid accidental data loss.")
    public ToolCallResult del(
            @Param(name = "keys", description = "Redis key deletion is disabled by the default safety policy") String keys
    ) {
        return ToolCallResult.error("DEL is disabled by the MCP Redis safety policy");
    }

    /**
     * 按模式匹配查找键。
     * <p>
     * 等效于 Redis 命令 {@code KEYS pattern}。
     * 支持 glob 风格的模式匹配，例如 {@code user:*}、{@code session:?} 等。
     * </p>
     * <p><b>注意：</b>在生产环境中应谨慎使用，键数量较大时可能影响性能。</p>
     *
     * @param pattern 匹配模式，支持通配符 {@code *}、{@code ?}、{@code [abc]} 等
     * @return 匹配的键名集合，以 JSON 数组格式返回
     */
    @McpTool(name = "keys", description = "Find up to 100 Redis keys with a narrow namespaced pattern such as user:* or cache:profile:*. Broad patterns like * are rejected.")
    public ToolCallResult keys(
            @Param(name = "pattern", description = "Narrow Redis glob pattern, for example user:* or cache:profile:*") String pattern
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            requireNarrowKeyPattern(pattern);
            Set<String> keys = new LinkedHashSet<>();
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanParams params = new ScanParams().match(pattern).count(MAX_KEYS_RETURNED);
            do {
                ScanResult<String> result = jedis.scan(cursor, params);
                keys.addAll(result.getResult());
                cursor = result.getCursor();
            } while (!ScanParams.SCAN_POINTER_START.equals(cursor) && keys.size() < MAX_KEYS_RETURNED);
            if (keys.size() > MAX_KEYS_RETURNED) {
                keys = new LinkedHashSet<>(new ArrayList<>(keys).subList(0, MAX_KEYS_RETURNED));
            }
            return ToolCallResult.json(keys);
        } catch (Exception e) {
            return ToolCallResult.error("KEYS failed: " + e.getMessage());
        }
    }

    /**
     * 获取键存储的数据类型。
     * <p>
     * 等效于 Redis 命令 {@code TYPE key}。
     * 返回值可能是 string、list、set、zset、hash 或 none（键不存在）。
     * </p>
     *
     * @param key 要查询的 Redis 键名
     * @return 数据类型名称（string/list/set/zset/hash/none）
     */
    @McpTool(name = "type", description = "Return the Redis data type stored at a key, such as string, list, set, zset, hash, or none. Use this before choosing a type-specific tool when the key structure is unknown.")
    public ToolCallResult type(
            @Param(name = "key", description = "Redis key to inspect") String key
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            requireSafeKey(key);
            String type = jedis.type(key);
            return ToolCallResult.success(type);
        } catch (Exception e) {
            return ToolCallResult.error("TYPE failed: " + e.getMessage());
        }
    }

    /**
     * 获取键的剩余生存时间。
     * <p>
     * 等效于 Redis 命令 {@code TTL key}。
     * 返回值说明：
     * <ul>
     *   <li>正整数 - 剩余秒数</li>
     *   <li>{@code -1} - 键存在但未设置过期时间</li>
     *   <li>{@code -2} - 键不存在</li>
     * </ul>
     * </p>
     *
     * @param key 要查询的 Redis 键名
     * @return 剩余生存时间的描述信息
     */
    @McpTool(name = "ttl", description = "Get the remaining time to live for a Redis key. Returns seconds, or explains when the key has no expiration or does not exist.")
    public ToolCallResult ttl(
            @Param(name = "key", description = "Redis key whose expiration should be checked") String key
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            requireSafeKey(key);
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

    /**
     * 获取哈希表中指定字段的值。
     * <p>等效于 Redis 命令 {@code HGET key field}。若字段不存在，返回 {@code "(nil)"}。</p>
     *
     * @param key   哈希表的键名
     * @param field 要查询的字段名
     * @return 字段对应的值，或 {@code "(nil)"}（字段不存在时）
     */
    @McpTool(name = "hget", description = "Get one field value from a Redis hash. Use this when the user asks for a specific property inside a hash key.")
    public ToolCallResult hget(
            @Param(name = "key", description = "Redis hash key to read") String key,
            @Param(name = "field", description = "Hash field name to read") String field
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            requireSafeKey(key);
            requireSafeField(field);
            String value = jedis.hget(key, field);
            if (value == null) {
                return ToolCallResult.success("(nil)");
            }
            return ToolCallResult.success(value);
        } catch (Exception e) {
            return ToolCallResult.error("HGET failed: " + e.getMessage());
        }
    }

    /**
     * 设置哈希表中指定字段的值。
     * <p>等效于 Redis 命令 {@code HSET key field value}。若字段已存在则覆盖。</p>
     *
     * @param key   哈希表的键名
     * @param field 要设置的字段名
     * @param value 要存储的值
     * @return 操作结果，成功时返回 {@code "OK"}
     */
    @McpTool(name = "hset", description = "Set one field value in a Redis hash. Use this for structured object-like data instead of overwriting the whole key as a string.")
    public ToolCallResult hset(
            @Param(name = "key", description = "Redis hash key to write") String key,
            @Param(name = "field", description = "Hash field name to set") String field,
            @Param(name = "value", description = "String value to store in the hash field") String value
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            requireSafeKey(key);
            requireSafeField(field);
            jedis.hset(key, field, value);
            return ToolCallResult.success("OK");
        } catch (Exception e) {
            return ToolCallResult.error("HSET failed: " + e.getMessage());
        }
    }

    /**
     * 获取哈希表中所有字段和值。
     * <p>等效于 Redis 命令 {@code HGETALL key}。返回字段名与值的完整映射。</p>
     *
     * @param key 哈希表的键名
     * @return 字段-值映射，以 JSON 对象格式返回
     */
    @McpTool(name = "hgetall", description = "Get all fields and values from a small Redis hash as JSON. Hashes with more than 100 fields are rejected; use hget for specific fields.")
    public ToolCallResult hgetall(
            @Param(name = "key", description = "Redis hash key to read") String key
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            requireSafeKey(key);
            long fieldCount = jedis.hlen(key);
            if (fieldCount > MAX_COLLECTION_ITEMS) {
                return ToolCallResult.error("HGETALL rejected: hash has more than " + MAX_COLLECTION_ITEMS + " fields; use hget for specific fields");
            }
            Map<String, String> entries = jedis.hgetAll(key);
            return ToolCallResult.json(entries);
        } catch (Exception e) {
            return ToolCallResult.error("HGETALL failed: " + e.getMessage());
        }
    }

    /**
     * 获取列表中指定索引范围的元素。
     * <p>
     * 等效于 Redis 命令 {@code LRANGE key start stop}。
     * 索引从 0 开始，支持负数索引（{@code -1} 表示最后一个元素）。
     * </p>
     *
     * @param key   列表的键名
     * @param start 起始索引（包含）
     * @param stop  结束索引（包含）
     * @return 指定范围内的元素列表，以 JSON 数组格式返回
     */
    @McpTool(name = "lrange", description = "Get up to 100 elements from a Redis list by inclusive index range. Full-list reads such as 0 to -1 are rejected; use llen first.")
    public ToolCallResult lrange(
            @Param(name = "key", description = "Redis list key to read") String key,
            @Param(name = "start", description = "Inclusive start index, zero-based; negative values count from the end") long start,
            @Param(name = "stop", description = "Inclusive stop index; use a bounded range") long stop
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            requireSafeKey(key);
            requireSafeRange(start, stop);
            List<String> values = jedis.lrange(key, start, stop);
            return ToolCallResult.json(values);
        } catch (Exception e) {
            return ToolCallResult.error("LRANGE failed: " + e.getMessage());
        }
    }

    /**
     * 获取列表的长度。
     * <p>等效于 Redis 命令 {@code LLEN key}。若键不存在返回 0。</p>
     *
     * @param key 列表的键名
     * @return 列表长度
     */
    @McpTool(name = "llen", description = "Get the number of elements in a Redis list. Use this before lrange when deciding a safe range to read.")
    public ToolCallResult llen(
            @Param(name = "key", description = "Redis list key whose length should be checked") String key
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            requireSafeKey(key);
            long length = jedis.llen(key);
            return ToolCallResult.success(String.valueOf(length));
        } catch (Exception e) {
            return ToolCallResult.error("LLEN failed: " + e.getMessage());
        }
    }

    /**
     * 获取集合中的成员数量。
     * <p>等效于 Redis 命令 {@code SCARD key}。若键不存在返回 0。</p>
     *
     * @param key 集合的键名
     * @return 集合成员数量
     */
    @McpTool(name = "scard", description = "Get the number of members in a Redis set. Use this to count unique members without retrieving the full set.")
    public ToolCallResult scard(
            @Param(name = "key", description = "Redis set key whose member count should be checked") String key
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            requireSafeKey(key);
            long count = jedis.scard(key);
            return ToolCallResult.success(String.valueOf(count));
        } catch (Exception e) {
            return ToolCallResult.error("SCARD failed: " + e.getMessage());
        }
    }

    /**
     * 获取集合中的所有成员。
     * <p>等效于 Redis 命令 {@code SMEMBERS key}。</p>
     *
     * @param key 集合的键名
     * @return 集合所有成员，以 JSON 数组格式返回
     */
    @McpTool(name = "smembers", description = "Get all members from a small Redis set as JSON. Sets with more than 100 members are rejected; use scard for counts.")
    public ToolCallResult smembers(
            @Param(name = "key", description = "Redis set key to read") String key
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            requireSafeKey(key);
            long memberCount = jedis.scard(key);
            if (memberCount > MAX_COLLECTION_ITEMS) {
                return ToolCallResult.error("SMEMBERS rejected: set has more than " + MAX_COLLECTION_ITEMS + " members; use scard for counts");
            }
            Set<String> members = jedis.smembers(key);
            return ToolCallResult.json(members);
        } catch (Exception e) {
            return ToolCallResult.error("SMEMBERS failed: " + e.getMessage());
        }
    }

    /**
     * 获取 Redis 服务器信息。
     * <p>
     * 等效于 Redis 命令 {@code INFO [section]}。
     * 可选的 section 参数包括：server、clients、memory、stats、replication、
     * cpu、keyspace 等，不指定则返回全部信息。
     * </p>
     *
     * @param section 信息分类，可选；为空时返回全部信息
     * @return 服务器信息文本
     */
    @McpTool(name = "info", description = "Get Redis server INFO output for a safe diagnostic section such as server, clients, memory, stats, keyspace, or cpu. A section is required.")
    public ToolCallResult info(
            @Param(name = "section", description = "Required safe Redis INFO section name, for example server, memory, stats, keyspace, or cpu", required = false) String section
    ) {
        try (Jedis jedis = jedisPool.getResource()) {
            requireSafeInfoSection(section);
            String info = jedis.info(section.trim().toLowerCase(Locale.ROOT));
            return ToolCallResult.success(info);
        } catch (Exception e) {
            return ToolCallResult.error("INFO failed: " + e.getMessage());
        }
    }

    /**
     * 获取当前数据库中的键总数。
     * <p>等效于 Redis 命令 {@code DBSIZE}。</p>
     *
     * @return 当前数据库的键数量
     */
    @McpTool(name = "dbsize", description = "Get the number of keys in the currently selected Redis database. Use this for a quick database-size summary without listing keys.")
    public ToolCallResult dbsize() {
        try (Jedis jedis = jedisPool.getResource()) {
            long size = jedis.dbSize();
            return ToolCallResult.success(String.valueOf(size));
        } catch (Exception e) {
            return ToolCallResult.error("DBSIZE failed: " + e.getMessage());
        }
    }

    static void requireSafeKey(String key) {
        requireSafeText(key, MAX_KEY_LENGTH, "Redis key");
    }

    static void requireSafeField(String field) {
        requireSafeText(field, MAX_FIELD_LENGTH, "Redis hash field");
    }

    static void requireSafeTtl(long ttl) {
        if (ttl <= 0 || ttl > MAX_TTL_SECONDS) {
            throw new IllegalArgumentException("TTL must be between 1 second and " + MAX_TTL_SECONDS + " seconds");
        }
    }

    static void requireNarrowKeyPattern(String pattern) {
        requireSafeText(pattern, MAX_KEY_LENGTH, "Redis key pattern");
        String trimmed = pattern.trim();
        if (trimmed.equals("*") || trimmed.startsWith("*") || trimmed.startsWith("?") || trimmed.startsWith("[")) {
            throw new IllegalArgumentException("broad key patterns are not allowed");
        }
        int firstWildcard = firstWildcardIndex(trimmed);
        if (firstWildcard >= 0) {
            String prefix = trimmed.substring(0, firstWildcard);
            if (prefix.length() < 3 || !prefix.contains(":")) {
                throw new IllegalArgumentException("wildcard patterns must use a narrow namespace prefix such as user:*");
            }
        }
    }

    static void requireSafeRange(long start, long stop) {
        if (start == 0 && stop == -1) {
            throw new IllegalArgumentException("full-list reads are not allowed");
        }
        if (stop >= start && stop - start + 1 <= MAX_COLLECTION_ITEMS) {
            return;
        }
        if (stop == -1 && start < 0 && Math.abs(start) <= MAX_COLLECTION_ITEMS) {
            return;
        }
        throw new IllegalArgumentException("range must read at most " + MAX_COLLECTION_ITEMS + " items");
    }

    static void requireSafeInfoSection(String section) {
        if (section == null || section.isBlank()) {
            throw new IllegalArgumentException("INFO section is required");
        }
        String normalized = section.trim().toLowerCase(Locale.ROOT);
        if (!SAFE_INFO_SECTIONS.contains(normalized)) {
            throw new IllegalArgumentException("INFO section is not allowed: " + section);
        }
    }

    private static void requireSafeText(String value, int maxLength, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(label + " must not contain leading or trailing whitespace");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(label + " must not exceed " + maxLength + " characters");
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                throw new IllegalArgumentException(label + " must not contain control characters");
            }
        }
    }

    private static int firstWildcardIndex(String pattern) {
        int first = -1;
        for (char wildcard : new char[]{'*', '?', '['}) {
            int index = pattern.indexOf(wildcard);
            if (index >= 0 && (first < 0 || index < first)) {
                first = index;
            }
        }
        return first;
    }

    /**
     * 关闭 Redis 连接池，释放所有连接资源。
     * <p>应在 MCP Server 停止时调用，通常通过 JVM 关闭钩子触发。</p>
     */
    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            log.info("Redis connection pool closed");
        }
    }
}
