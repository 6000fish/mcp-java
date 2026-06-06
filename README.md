<p align="center">
  <a href="./README.md">English</a> | <a href="./README.zh-CN.md">中文</a>
</p>

# MCP Java SDK

[![Maven Central](https://img.shields.io/maven-central/v/io.github.6000fish/mcp-sdk.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.6000fish/mcp-sdk)
[![Release](https://img.shields.io/github/v/release/6000fish/mcp-java?label=GitHub%20Release)](https://github.com/6000fish/mcp-java/releases)
[![CI](https://github.com/6000fish/mcp-java/actions/workflows/ci.yml/badge.svg)](https://github.com/6000fish/mcp-java/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17+-green.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

> Java SDK for building custom [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) servers quickly, with runnable MySQL and Redis server examples.

MCP.so listings: [MySQL MCP Server for Java](https://mcp.so/zh/server/mysql-mcp-server-for-java/6000fish) · [Redis MCP Server for Java](https://mcp.so/zh/server/redis-mcp-server-for-java/6000fish)

## Why this project

- Build custom MCP servers in Java with a small SDK.
- Use stdio for local Agent integration and SSE for HTTP-based integrations.
- Define tools, resources, and prompts with either Java code or annotations.
- Start from copyable examples instead of wiring JSON-RPC by hand.
- Use MySQL and Redis servers as ready-to-run examples for real data sources.

## 5-minute paths

| Goal | Start here | Requires external service |
|------|------------|---------------------------|
| Run the fastest SDK demo | [`mcp-examples/quick-start`](mcp-examples/quick-start) | No |
| Copy a custom server template | [`mcp-examples/custom-server-template`](mcp-examples/custom-server-template) | No |
| Build inside Spring Boot | [`mcp-examples/spring-boot-example`](mcp-examples/spring-boot-example) | No |
| Connect Redis to an Agent | [`docs/redis-server.md`](docs/redis-server.md) | Redis |
| Connect MySQL to an Agent | [`docs/mysql-server.md`](docs/mysql-server.md) | MySQL |

Full walkthrough: [5-Minute Quick Start](docs/quickstart.md).

## Install from Maven Central

Core SDK:

```xml
<dependency>
    <groupId>io.github.6000fish</groupId>
    <artifactId>mcp-sdk</artifactId>
    <version>0.1.1</version>
</dependency>
```

Spring Boot starter:

```xml
<dependency>
    <groupId>io.github.6000fish</groupId>
    <artifactId>mcp-spring-boot-starter</artifactId>
    <version>0.1.1</version>
</dependency>
```

## Fastest SDK demo

Build the no-database quick-start server:

```bash
mvn package -pl mcp-examples/quick-start -am -DskipTests
```

Add the runnable jar to your MCP Agent configuration:

```json
{
  "mcpServers": {
    "quick-start": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-java/mcp-examples/quick-start/target/quick-start-0.1.1.jar"
      ]
    }
  }
}
```

Restart the Agent and ask:

```text
Use the quick-start MCP server to greet Alice and get the current time.
```

The demo exposes `greet`, `current_time`, `calculate`, `text_transform`, and resource `server://info`.

## Build your own MCP server

For a copyable annotation-based template:

```bash
mvn package -pl mcp-examples/custom-server-template -am -DskipTests
```

Configure the generated jar:

```json
{
  "mcpServers": {
    "custom-template": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-java/mcp-examples/custom-server-template/target/custom-server-template-0.1.1.jar"
      ]
    }
  }
}
```

Then edit [`CustomMcpServer.java`](mcp-examples/custom-server-template/src/main/java/com/mcp/example/template/CustomMcpServer.java) and add your own `@McpTool`, `@McpResource`, or `@McpPrompt` methods.

Minimal SDK API example:

```java
McpServer server = DefaultMcpServer.builder()
        .name("my-server")
        .version("1.0.0")
        .build();

server.tool("greet", "Greet someone", arguments -> {
    String name = (String) arguments.getOrDefault("name", "World");
    return ToolCallResult.success("Hello, " + name + "!");
});

server.start(new StdioTransport());
```

Annotation example:

```java
@McpServer(name = "my-server", version = "1.0.0")
public class MyServer {

    @McpTool(name = "hello", description = "Say hello")
    public String hello(@Param(name = "name") String name) {
        return "Hello, " + name + "!";
    }
}
```

## Ready-to-use servers

Build the currently published ready-to-use server modules:

```bash
mvn package -pl mcp-server-collection/mcp-server-mysql -am -DskipTests
mvn package -pl mcp-server-collection/mcp-server-redis -am -DskipTests
```

MySQL server config:

```json
{
  "mcpServers": {
    "mysql": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-java/mcp-server-collection/mcp-server-mysql/target/mcp-server-mysql-0.1.1.jar"
      ],
      "env": {
        "MYSQL_HOST": "localhost",
        "MYSQL_PORT": "3306",
        "MYSQL_DATABASE": "mcp_demo",
        "MYSQL_USERNAME": "my_user",
        "MYSQL_PASSWORD": "your_password"
      }
    }
  }
}
```

Redis server config:

```json
{
  "mcpServers": {
    "redis": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-java/mcp-server-collection/mcp-server-redis/target/mcp-server-redis-0.1.1.jar"
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

Before connecting real data, read the [Security Guide](docs/security.md). Keep credentials in local Agent configuration or environment variables only.

## Available server tools

| Server | Description | Tools |
|--------|-------------|-------|
| MySQL | Database operations | query, execute, list_databases, list_tables, describe_table, explain_query, get_table_status |
| Redis | Cache operations | get, set, del, keys, type, ttl, hget, hset, hgetall, lrange, llen, scard, smembers, info, dbsize |

## Documentation

User documentation:

- [5-Minute Quick Start](docs/quickstart.md)
- [MySQL Server Guide](docs/mysql-server.md)
- [Redis Server Guide](docs/redis-server.md)
- [Security Guide](docs/security.md)
- [Troubleshooting](docs/troubleshooting.md)
- [Changelog](CHANGELOG.md)

Maintainer documentation:

- [Release Guide](docs/release.md)
- [MCP Directory Submission Materials](docs/mcp-directory-submission.md)

## Compatibility

The stdio servers are designed for mainstream MCP clients and Agents:

- Claude Code: verified with MySQL and Redis stdio servers.
- Codex: verified with MySQL stdio server, including `tools/list`, `tools/call`, and client `_meta` fields.
- Other MCP clients such as opencode, OpenClaw, Gemini-related tooling: use the same stdio JSON-RPC flow and should use the documented config shape.

Compatibility safeguards include required `inputSchema` on tools, tolerant request parsing for client extension fields such as `_meta`, omitted null fields in stdio responses, and logging to stderr so stdout stays reserved for MCP JSON-RPC messages.

## Project structure

```text
mcp-java/
├── mcp-sdk/                      # Core SDK
├── mcp-spring-boot-starter/      # Spring Boot integration
├── mcp-server-collection/        # Ready-to-use server implementations
└── mcp-examples/                 # SDK examples and templates
```

## Build from source

```bash
git clone https://github.com/6000fish/mcp-java.git
cd mcp-java
mvn clean install
```

## Community

- [Contributing Guide](CONTRIBUTING.md)
- [Security Policy](SECURITY.md)
- [Code of Conduct](CODE_OF_CONDUCT.md)
- [Changelog](CHANGELOG.md)

## Maintainers

Release instructions are in [docs/release.md](docs/release.md). Do not commit Maven Central credentials, GPG passphrases, private keys, or `settings.xml`.

## License

[MIT](LICENSE)
