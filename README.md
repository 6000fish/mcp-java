<p align="center">
  <a href="./README.md">English</a> | <a href="./README.zh-CN.md">中文</a>
</p>

# MCP Java SDK

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-green.svg)](https://www.oracle.com/java/)
[![CI](https://github.com/6000fish/mcp-java/actions/workflows/ci.yml/badge.svg)](https://github.com/6000fish/mcp-java/actions/workflows/ci.yml)

> Java implementation of [Model Context Protocol (MCP)](https://modelcontextprotocol.io/)

## Features

- 🚀 **Easy to Use** - Simple API for building MCP servers
- 🔌 **Multiple Transports** - Stdio and SSE support
- 🛠️ **Annotation-Driven** - Use annotations to define tools, resources, and prompts
- 📦 **Spring Boot Integration** - Auto-configuration support
- 🎯 **Java SDK First** - Build custom MCP servers quickly, with MySQL and Redis runnable examples

## Quick Start

### Add Dependency

```xml
<dependency>
    <groupId>io.github.6000fish</groupId>
    <artifactId>mcp-sdk</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Create a Server

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

### Configure an MCP Client

Add this server as a stdio MCP server in your Agent configuration:

```json
{
  "mcpServers": {
    "my-server": {
      "type": "stdio",
      "command": "java",
      "args": ["-jar", "my-server.jar"]
    }
  }
}
```

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

## Server Examples

| Example | Path | What it shows |
|---------|------|---------------|
| Manual SDK server | `mcp-examples/quick-start` | Register tools/resources directly with `DefaultMcpServer` and start with stdio |
| Custom business server | `mcp-examples/business-server-example` | Build an annotation-based business MCP Server without Spring |
| Spring Boot server | `mcp-examples/spring-boot-example` | Use `@McpServer`, `@McpTool`, and Spring Boot auto-configuration |
| MySQL server | `mcp-server-collection/mcp-server-mysql` | Run a ready-to-use database MCP Server configured by env vars |
| Redis server | `mcp-server-collection/mcp-server-redis` | Run a ready-to-use cache MCP Server configured by env vars |

Build and run the custom business server example:

```bash
mvn package -pl mcp-examples/business-server-example -am -DskipTests
java -jar mcp-examples/business-server-example/target/business-server-example-0.1.0.jar
```

Agent config example:

```json
{
  "mcpServers": {
    "business": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/business-server-example-0.1.0.jar"
      ]
    }
  }
}
```

Use `quick-start` for manual SDK registration, `business-server-example` for annotation-based custom stdio servers, `spring-boot-example` for Spring Boot apps, and MySQL/Redis modules for ready-to-use data source servers.

### Run Ready-to-Use Servers

Build the server collection modules:

```bash
mvn package -pl mcp-server-collection/mcp-server-mysql -am -DskipTests
mvn package -pl mcp-server-collection/mcp-server-redis -am -DskipTests
```

MySQL server example:

```json
{
  "mcpServers": {
    "mysql": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-java/mcp-server-collection/mcp-server-mysql/target/mcp-server-mysql-0.1.0.jar"
      ],
      "env": {
        "MYSQL_HOST": "localhost",
        "MYSQL_PORT": "3306",
        "MYSQL_DATABASE": "my_database",
        "MYSQL_USERNAME": "my_user",
        "MYSQL_PASSWORD": "your_password"
      }
    }
  }
}
```

Redis server example:

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

Keep real passwords in your local Agent configuration or environment variables only. In stdio mode, stdout is reserved for MCP JSON-RPC messages and server logs are written to stderr.

## Spring Boot Integration

```xml
<dependency>
    <groupId>io.github.6000fish</groupId>
    <artifactId>mcp-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

```java
@Component
@McpServer(name = "my-server", version = "1.0.0")
public class MyServer {

    @McpTool(name = "hello", description = "Say hello")
    public String hello(@Param(name = "name") String name) {
        return "Hello, " + name + "!";
    }
}
```

## Available Servers

| Server | Description | Tools |
|--------|-------------|-------|
| MySQL | Database operations | query, execute, list_databases, list_tables, describe_table, explain_query, get_table_status |
| Redis | Cache operations | get, set, del, keys, type, ttl, hget, hset, hgetall, lrange, llen, scard, smembers, info, dbsize |

## Project Structure

```
mcp-java/
├── mcp-sdk/                      # Core SDK
├── mcp-spring-boot-starter/      # Spring Boot integration
├── mcp-server-collection/        # Server implementations
└── mcp-examples/                 # Examples
```

## Building

```bash
git clone https://github.com/6000fish/mcp-java.git
cd mcp-java
mvn clean install
```

## Publishing

Before publishing to Maven Central, make sure the `io.github.6000fish` namespace is verified in your Central/Sonatype account. The first public release version is `0.1.0`.

Configure your local `~/.m2/settings.xml` with the `central` server credentials generated from a Central Portal token and a local GPG signing key. Never commit Maven Central credentials, GPG passphrases, private keys, or `settings.xml`.

Central Portal upload command for the first public Maven Central scope:

```bash
mvn clean deploy -Prelease -DskipTests -pl .,mcp-sdk,mcp-spring-boot-starter,mcp-server-collection,mcp-server-collection/mcp-server-common,mcp-server-collection/mcp-server-mysql,mcp-server-collection/mcp-server-redis
```

## Community

- [Contributing Guide](CONTRIBUTING.md)
- [Security Policy](SECURITY.md)
- [Code of Conduct](CODE_OF_CONDUCT.md)
- [Changelog](CHANGELOG.md)

## License

[MIT](LICENSE)
