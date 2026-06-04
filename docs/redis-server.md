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

## Tools

| Tool | Description |
|---|---|
| `get` | Get a string key value |
| `set` | Set a string key value, optionally with TTL |
| `hget` | Read one hash field |
| `hset` | Write one hash field |
| `hgetall` | Read a small hash as JSON |
| `keys` | Find up to 100 keys with a narrow namespaced pattern |
| `type` | Return the data type stored at a key |
| `ttl` | Return key TTL information |
| `lrange` | Read a bounded Redis list range |
| `llen` | Count Redis list elements |
| `scard` | Count Redis set members |
| `smembers` | Read a small Redis set |
| `info` | Read a safe Redis INFO section |
| `dbsize` | Count keys in the current Redis database |

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

- `del` is disabled by default to avoid accidental data loss.
- Broad key patterns such as `*` are rejected.
- Large collection reads are limited.
- Control characters and oversized keys/fields are rejected.
