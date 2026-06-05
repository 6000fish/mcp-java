# Redis MCP Server 使用手册

中文 | [English](../redis-server.md)

Redis MCP Server 通过 stdio 将常用 Redis 操作暴露为 MCP 工具。

## 构建

```bash
mvn package -pl mcp-server-collection/mcp-server-redis -am -DskipTests
```

可执行 jar 生成位置：

```text
mcp-server-collection/mcp-server-redis/target/mcp-server-redis-0.1.0.jar
```

## 配置

环境变量：

| 变量 | 必填 | 默认值 | 说明 |
|---|---:|---|---|
| `REDIS_HOST` | 否 | `localhost` | Redis 地址 |
| `REDIS_PORT` | 否 | `6379` | Redis 端口 |
| `REDIS_PASSWORD` | 否 | 空 | Redis 密码 |

## Agent 配置

```json
{
  "mcpServers": {
    "redis": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-java/mcp-server-collection/mcp-server-redis/target/mcp-server-redis-0.1.0.jar"
      ],
      "env": {
        "REDIS_HOST": "localhost",
        "REDIS_PORT": "6379",
        "REDIS_PASSWORD": ""
      }
    }
  }
}
```

## 工具

| 工具 | 说明 |
|---|---|
| `get(key)` | 获取字符串键的值 |
| `set(key, value, ttl?)` | 设置字符串键值，可选 TTL |
| `del(keys)` | 已注册，但默认安全策略下禁用 |
| `keys(pattern)` | 使用窄命名空间模式查找最多 100 个键 |
| `type(key)` | 返回 key 中存储的数据类型 |
| `ttl(key)` | 返回 key 的 TTL 信息 |
| `hget(key, field)` | 读取一个 hash 字段 |
| `hset(key, field, value)` | 写入一个 hash 字段 |
| `hgetall(key)` | 以 JSON 读取小型 hash |
| `lrange(key, start, stop)` | 读取有边界的 Redis list 范围 |
| `llen(key)` | 统计 Redis list 元素数量 |
| `scard(key)` | 统计 Redis set 成员数量 |
| `smembers(key)` | 读取小型 Redis set |
| `info(section)` | 读取安全的 Redis INFO section |
| `dbsize()` | 统计当前 Redis 数据库中的 key 数量 |

## 示例 Prompt

```text
Set Redis key demo:greeting to hello-mcp and read it back.
```

```text
Show me the Redis server memory info.
```

```text
Find keys under the demo:* namespace.
```

## 安全行为

- `del(keys)` 会注册给客户端看到安全策略，但默认禁用并始终拒绝删除操作。
- 拒绝 `*`、前置通配符和过短通配符前缀等宽泛 key pattern；建议使用 `demo:*` 这类命名空间模式。
- `keys(pattern)` 最多返回 100 个 key。
- `hgetall`、`lrange` 和 `smembers` 最多读取 100 个集合元素。
- `set(key, value, ttl?)` 接受 1 秒到 30 天之间的 TTL。
- `info(section)` 必须指定 `server`、`clients`、`memory`、`stats`、`keyspace` 或 `cpu`。
- 拒绝控制字符和超长 key/field；key、field 和 pattern 限制为 512 字符。
