# Redis MCP Server Guide

[中文](zh-CN/redis-server.md) | English

The Redis MCP Server exposes common Redis operations as MCP tools over stdio.

## Build

```bash
mvn package -pl mcp-server-collection/mcp-server-redis -am -DskipTests
```

The executable jar is generated at:

```text
mcp-server-collection/mcp-server-redis/target/mcp-server-redis-0.1.0.jar
```

## Configuration

Environment variables:

| Variable | Required | Default | Description |
|---|---:|---|---|
| `REDIS_HOST` | No | `localhost` | Redis host |
| `REDIS_PORT` | No | `6379` | Redis port |
| `REDIS_PASSWORD` | No | empty | Redis password |

## Agent configuration

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

## Tools

| Tool | Description |
|---|---|
| `get(key)` | Get a string key value |
| `set(key, value, ttl?)` | Set a string key value, optionally with TTL |
| `del(keys)` | Registered but disabled by default safety policy |
| `keys(pattern)` | Find up to 100 keys with a narrow namespaced pattern |
| `type(key)` | Return the data type stored at a key |
| `ttl(key)` | Return key TTL information |
| `hget(key, field)` | Read one hash field |
| `hset(key, field, value)` | Write one hash field |
| `hgetall(key)` | Read a small hash as JSON |
| `lrange(key, start, stop)` | Read a bounded Redis list range |
| `llen(key)` | Count Redis list elements |
| `scard(key)` | Count Redis set members |
| `smembers(key)` | Read a small Redis set |
| `info(section)` | Read a safe Redis INFO section |
| `dbsize()` | Count keys in the current Redis database |

## Example prompts

```text
Set Redis key demo:greeting to hello-mcp and read it back.
```

```text
Show me the Redis server memory info.
```

```text
Find keys under the demo:* namespace.
```

## Safety behavior

- `del(keys)` is registered so clients can see the safety policy, but deletion is disabled by default and always rejected.
- Broad key patterns such as `*`, leading wildcards, and very short wildcard prefixes are rejected; use namespaced patterns such as `demo:*`.
- `keys(pattern)` returns at most 100 keys.
- `hgetall`, `lrange`, and `smembers` read at most 100 collection items.
- `set(key, value, ttl?)` accepts TTL values from 1 second to 30 days.
- `info(section)` requires one of `server`, `clients`, `memory`, `stats`, `keyspace`, or `cpu`.
- Control characters and oversized keys/fields are rejected; keys, fields, and patterns are limited to 512 characters.
