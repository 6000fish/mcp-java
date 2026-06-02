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
git clone https://github.com/yourusername/mcp-java.git
cd mcp-java
mvn clean install
```

## Contributing

Contributions are welcome! Please read our [Contributing Guide](CONTRIBUTING.md).

## License

[MIT](LICENSE)
