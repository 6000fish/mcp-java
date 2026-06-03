<p align="center">
  <a href="./README.md">English</a> | <a href="./README.zh-CN.md">中文</a>
</p>

# MCP Java SDK

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-green.svg)](https://www.oracle.com/java/)

> Java implementation of [Model Context Protocol (MCP)](https://modelcontextprotocol.io/)

## Features

- 🚀 **Easy to Use** - Simple API for building MCP servers
- 🔌 **Multiple Transports** - Stdio and SSE support
- 🛠️ **Annotation-Driven** - Use annotations to define tools
- 📦 **Spring Boot Integration** - Auto-configuration support
- 🎯 **Ready-to-Use Servers** - MySQL, Redis, Filesystem servers included

## Quick Start

### Add Dependency

```xml
<dependency>
    <groupId>com.mcp</groupId>
    <artifactId>mcp-sdk</artifactId>
    <version>1.0.0</version>
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

### Configure Claude Desktop

Add to `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "my-server": {
      "command": "java",
      "args": ["-jar", "my-server.jar"]
    }
  }
}
```

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
java -jar mcp-examples/business-server-example/target/business-server-example-1.0.0-SNAPSHOT.jar
```

Agent config example:

```json
{
  "mcpServers": {
    "business": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/business-server-example-1.0.0-SNAPSHOT.jar"
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
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-server-mysql-1.0.0-SNAPSHOT.jar"
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
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-server-redis-1.0.0-SNAPSHOT.jar"
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
    <groupId>com.mcp</groupId>
    <artifactId>mcp-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
@Component
@McpServer(name = "my-server", version = "1.0.0")
public class MyServer {

    @McpTool(name = "hello", description = "Say hello")
    public String hello(@Param("name") String name) {
        return "Hello, " + name + "!";
    }
}
```

## Available Servers

| Server | Description | Tools |
|--------|-------------|-------|
| MySQL | Database operations | query, list_tables, describe_table |
| Redis | Cache operations | get, set, hget, hgetall, keys |
| Filesystem | File operations | read, write, list, search |

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

Before publishing to Maven Central, make sure the `com.mcp` namespace is verified in your Central/Sonatype account, then change `1.0.0-SNAPSHOT` to a release version.

Configure your local `~/.m2/settings.xml` with the `ossrh` server credentials and a local GPG signing key. Never commit Maven Central credentials, GPG passphrases, private keys, or `settings.xml`.

Release build command:

```bash
mvn clean deploy -Prelease -DskipTests
```

## Contributing

Contributions are welcome! Please read our [Contributing Guide](CONTRIBUTING.md).

## License

[MIT](LICENSE)
