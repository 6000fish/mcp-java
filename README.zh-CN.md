<p align="center">
  <a href="./README.md">English</a> | <a href="./README.zh-CN.md">中文</a>
</p>

# MCP Java SDK

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-green.svg)](https://www.oracle.com/java/)

> [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) 的 Java 实现

## 特性

- 🚀 **简单易用** - 简洁的 API，快速构建 MCP 服务
- 🔌 **多传输方式** - 支持 Stdio 和 SSE
- 🛠️ **注解驱动** - 使用注解定义工具
- 📦 **Spring Boot 集成** - 自动配置支持
- 🎯 **开箱即用** - 内置 MySQL、Redis、文件系统等服务

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>com.mcp</groupId>
    <artifactId>mcp-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 创建服务

```java
McpServer server = DefaultMcpServer.builder()
        .name("my-server")
        .version("1.0.0")
        .build();

server.tool("greet", "问候某人", arguments -> {
    String name = (String) arguments.getOrDefault("name", "World");
    return ToolCallResult.success("你好, " + name + "!");
});

server.start(new StdioTransport());
```

### 配置 Claude Desktop

在 `claude_desktop_config.json` 中添加：

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

## Server 示例

| 示例 | 路径 | 说明 |
|------|------|------|
| 手动 SDK Server | `mcp-examples/quick-start` | 使用 `DefaultMcpServer` 手动注册工具/资源，并通过 stdio 启动 |
| 自定义业务 Server | `mcp-examples/business-server-example` | 不依赖 Spring，使用注解开发业务 MCP Server |
| Spring Boot Server | `mcp-examples/spring-boot-example` | 使用 `@McpServer`、`@McpTool` 和 Spring Boot 自动配置 |
| MySQL Server | `mcp-server-collection/mcp-server-mysql` | 使用环境变量配置并运行现成数据库 MCP Server |
| Redis Server | `mcp-server-collection/mcp-server-redis` | 使用环境变量配置并运行现成缓存 MCP Server |

构建并运行自定义业务 Server 示例：

```bash
mvn package -pl mcp-examples/business-server-example -am -DskipTests
java -jar mcp-examples/business-server-example/target/business-server-example-1.0.0-SNAPSHOT.jar
```

Agent 配置示例：

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

手动注册工具/资源用 `quick-start`，注解式自定义 stdio Server 用 `business-server-example`，Spring Boot 应用用 `spring-boot-example`，已有数据源接入用 MySQL/Redis 模块。

### 运行内置服务

构建 Server Collection 模块：

```bash
mvn package -pl mcp-server-collection/mcp-server-mysql -am -DskipTests
mvn package -pl mcp-server-collection/mcp-server-redis -am -DskipTests
```

MySQL 服务示例：

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

Redis 服务示例：

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

真实密码只放在本地 Agent 配置或环境变量中，不要提交到 Git。stdio 模式下 stdout 保留给 MCP JSON-RPC 协议消息，服务日志会写入 stderr。

## Spring Boot 集成

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

    @McpTool(name = "hello", description = "问候")
    public String hello(@Param("name") String name) {
        return "你好, " + name + "!";
    }
}
```

## 内置服务

| 服务 | 说明 | 工具 |
|------|------|------|
| MySQL | 数据库操作 | query, list_tables, describe_table |
| Redis | 缓存操作 | get, set, hget, hgetall, keys |
| Filesystem | 文件操作 | read, write, list, search |

## 项目结构

```
mcp-java/
├── mcp-sdk/                      # 核心 SDK
├── mcp-spring-boot-starter/      # Spring Boot 集成
├── mcp-server-collection/        # 服务实现
└── mcp-examples/                 # 示例
```

## 构建

```bash
git clone https://github.com/6000fish/mcp-java.git
cd mcp-java
mvn clean install
```

## 发布准备

发布到 Maven Central 前，先确认你的 Central/Sonatype 账号已经完成 `com.mcp` namespace 校验，然后将 `1.0.0-SNAPSHOT` 改为正式发布版本。

在本地 `~/.m2/settings.xml` 配置 `ossrh` server 凭据，并准备本地 GPG 签名 key。不要把 Maven Central 凭据、GPG passphrase、私钥或 `settings.xml` 提交到 Git。

发布构建命令：

```bash
mvn clean deploy -Prelease -DskipTests
```

## 贡献

欢迎贡献！请阅读 [贡献指南](CONTRIBUTING.md)。

## 许可证

[MIT](LICENSE)
