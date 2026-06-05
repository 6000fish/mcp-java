<p align="center">
  <a href="./README.md">English</a> | <a href="./README.zh-CN.md">中文</a>
</p>

# MCP Java SDK

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-green.svg)](https://www.oracle.com/java/)
[![CI](https://github.com/6000fish/mcp-java/actions/workflows/ci.yml/badge.svg)](https://github.com/6000fish/mcp-java/actions/workflows/ci.yml)

> [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) 的 Java 实现

## 特性

- 🚀 **简单易用** - 简洁的 API，快速构建 MCP 服务
- 🔌 **多传输方式** - 支持 Stdio 和 SSE
- 🛠️ **注解驱动** - 使用注解定义工具
- 📦 **Spring Boot 集成** - 自动配置支持
- 🎯 **SDK 优先** - 快速构建自定义 MCP Server，并提供 MySQL、Redis 可运行示例

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.github.6000fish</groupId>
    <artifactId>mcp-sdk</artifactId>
    <version>0.1.0</version>
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

### 配置 MCP 客户端

在你的 Agent 配置中添加 stdio MCP Server：

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

## 文档

用户文档：

- [5 分钟快速开始](docs/zh-CN/quickstart.md)
- [MySQL Server 使用手册](docs/zh-CN/mysql-server.md)
- [Redis Server 使用手册](docs/zh-CN/redis-server.md)
- [安全说明](docs/zh-CN/security.md)
- [故障排查](docs/zh-CN/troubleshooting.md)
- [更新日志](CHANGELOG.md)

维护者文档：

- [发布手册](docs/zh-CN/release.md)
- [MCP 市场提交材料](docs/zh-CN/mcp-directory-submission.md)

## 兼容性

stdio Server 面向主流 MCP 客户端和 Agent 设计：

- Claude Code：已验证 MySQL 和 Redis stdio Server。
- Codex：已验证 MySQL stdio Server，包括 `tools/list`、`tools/call` 和客户端 `_meta` 字段。
- opencode、OpenClaw、Gemini 相关工具等其他 MCP 客户端：使用相同的 stdio JSON-RPC 流程，建议按本文档中的配置结构接入。

兼容性保障包括：工具定义始终返回必需的 `inputSchema`，请求解析兼容 `_meta` 等客户端扩展字段，stdio 响应省略 null 字段，并且日志写入 stderr，stdout 只保留给 MCP JSON-RPC 协议消息。

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
java -jar mcp-examples/business-server-example/target/business-server-example-0.1.0.jar
```

Agent 配置示例：

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

Redis 服务示例：

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

真实密码只放在本地 Agent 配置或环境变量中，不要提交到 Git。stdio 模式下 stdout 保留给 MCP JSON-RPC 协议消息，服务日志会写入 stderr。

## Spring Boot 集成

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

发布到 Maven Central 前，先确认你的 Central/Sonatype 账号已经完成 `io.github.6000fish` namespace 校验。首个公开发布版本为 `0.1.0`。

在本地 `~/.m2/settings.xml` 配置从 Central Portal token 生成的 `central` server 凭据，并准备本地 GPG 签名 key。不要把 Maven Central 凭据、GPG passphrase、私钥或 `settings.xml` 提交到 Git。

首个公开版本的 Central Portal 上传命令：

```bash
mvn clean deploy -Prelease -DskipTests -pl .,mcp-sdk,mcp-spring-boot-starter,mcp-server-collection,mcp-server-collection/mcp-server-common,mcp-server-collection/mcp-server-mysql,mcp-server-collection/mcp-server-redis
```

## 社区

- [贡献指南](CONTRIBUTING.md)
- [安全策略](SECURITY.md)
- [行为准则](CODE_OF_CONDUCT.md)
- [更新日志](CHANGELOG.md)

## 许可证

[MIT](LICENSE)
