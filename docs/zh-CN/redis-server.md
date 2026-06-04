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
        "/absolute/path/to/mcp-server-redis-0.1.0.jar"
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
| `get` | 获取字符串键的值 |
| `set` | 设置字符串键值，可选 TTL |
| `hget` | 读取一个 hash 字段 |
| `hset` | 写入一个 hash 字段 |
| `hgetall` | 以 JSON 读取小型 hash |
| `keys` | 使用窄命名空间模式查找最多 100 个键 |
| `type` | 返回 key 中存储的数据类型 |
| `ttl` | 返回 key 的 TTL 信息 |
| `lrange` | 读取有边界的 Redis list 范围 |
| `llen` | 统计 Redis list 元素数量 |
| `scard` | 统计 Redis set 成员数量 |
| `smembers` | 读取小型 Redis set |
| `info` | 读取安全的 Redis INFO section |
| `dbsize` | 统计当前 Redis 数据库中的 key 数量 |

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

- 默认禁用 `del`，避免误删数据。
- 拒绝 `*` 等宽泛 key pattern。
- 限制大集合读取。
- 拒绝控制字符和超长 key/field。
