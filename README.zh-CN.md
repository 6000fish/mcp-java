<p align="center">
  <a href="./README.md">English</a> | <a href="./README.zh-CN.md">中文</a>
</p>

# MCP Java SDK

[![Maven Central](https://img.shields.io/maven-central/v/io.github.6000fish/mcp-sdk.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.6000fish/mcp-sdk)
[![Release](https://img.shields.io/github/v/release/6000fish/mcp-java?label=GitHub%20Release)](https://github.com/6000fish/mcp-java/releases)
[![CI](https://github.com/6000fish/mcp-java/actions/workflows/ci.yml/badge.svg)](https://github.com/6000fish/mcp-java/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17+-green.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

> 用 Java 快速构建自定义 [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) Server 的 SDK，并提供 MySQL、Redis 可运行服务示例。

MCP.so 收录：[MySQL MCP Server for Java](https://mcp.so/zh/server/mysql-mcp-server-for-java/6000fish) · [Redis MCP Server for Java](https://mcp.so/zh/server/redis-mcp-server-for-java/6000fish)

## 为什么用这个项目

- 用一个轻量 SDK 构建自定义 Java MCP Server。
- 使用 stdio 接入本地 Agent，使用 SSE 支持 HTTP 场景。
- 通过 Java API 或注解定义工具、资源和 Prompt。
- 从可复制的示例开始，不需要手写 JSON-RPC 协议细节。
- MySQL 和 Redis Server 可作为真实数据源的可运行示例。

## 5 分钟路径

| 目标 | 从这里开始 | 是否需要外部服务 |
|------|------------|------------------|
| 运行最快 SDK demo | [`mcp-examples/quick-start`](mcp-examples/quick-start) | 不需要 |
| 复制自定义 Server 模板 | [`mcp-examples/custom-server-template`](mcp-examples/custom-server-template) | 不需要 |
| 在 Spring Boot 中构建 | [`mcp-examples/spring-boot-example`](mcp-examples/spring-boot-example) | 不需要 |
| 把 Redis 接入 Agent | [`docs/zh-CN/redis-server.md`](docs/zh-CN/redis-server.md) | Redis |
| 把 MySQL 接入 Agent | [`docs/zh-CN/mysql-server.md`](docs/zh-CN/mysql-server.md) | MySQL |

完整教程：[5 分钟快速开始](docs/zh-CN/quickstart.md)。

## 从 Maven Central 引入

核心 SDK：

```xml
<dependency>
    <groupId>io.github.6000fish</groupId>
    <artifactId>mcp-sdk</artifactId>
    <version>0.1.1</version>
</dependency>
```

Spring Boot Starter：

```xml
<dependency>
    <groupId>io.github.6000fish</groupId>
    <artifactId>mcp-spring-boot-starter</artifactId>
    <version>0.1.1</version>
</dependency>
```

## 最快 SDK demo

构建不依赖数据库的 quick-start Server：

```bash
mvn package -pl mcp-examples/quick-start -am -DskipTests
```

把生成的可执行 jar 加到 MCP Agent 配置中：

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

重启 Agent 后尝试：

```text
Use the quick-start MCP server to greet Alice and get the current time.
```

该 demo 暴露 `greet`、`current_time`、`calculate`、`text_transform`，以及资源 `server://info`。

## 构建你自己的 MCP Server

复制注解式模板：

```bash
mvn package -pl mcp-examples/custom-server-template -am -DskipTests
```

配置生成的 jar：

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

然后修改 [`CustomMcpServer.java`](mcp-examples/custom-server-template/src/main/java/com/mcp/example/template/CustomMcpServer.java)，添加你自己的 `@McpTool`、`@McpResource` 或 `@McpPrompt` 方法。

最小 SDK API 示例：

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

注解式示例：

```java
@McpServer(name = "my-server", version = "1.0.0")
public class MyServer {

    @McpTool(name = "hello", description = "问候")
    public String hello(@Param(name = "name") String name) {
        return "你好, " + name + "!";
    }
}
```

## Ready-to-use servers

构建当前已发布的 ready-to-use server 模块：

```bash
mvn package -pl mcp-server-collection/mcp-server-mysql -am -DskipTests
mvn package -pl mcp-server-collection/mcp-server-redis -am -DskipTests
```

MySQL Server 配置：

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

Redis Server 配置：

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

连接真实数据前请先阅读[安全说明](docs/zh-CN/security.md)。凭证只放在本地 Agent 配置或环境变量中。

## 可用服务工具

| 服务 | 说明 | 工具 |
|------|------|------|
| MySQL | 数据库操作 | query, execute, list_databases, list_tables, describe_table, explain_query, get_table_status |
| Redis | 缓存操作 | get, set, del, keys, type, ttl, hget, hset, hgetall, lrange, llen, scard, smembers, info, dbsize |

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

## 项目结构

```text
mcp-java/
├── mcp-sdk/                      # 核心 SDK
├── mcp-spring-boot-starter/      # Spring Boot 集成
├── mcp-server-collection/        # Ready-to-use Server 实现
└── mcp-examples/                 # SDK 示例和模板
```

## 从源码构建

```bash
git clone https://github.com/6000fish/mcp-java.git
cd mcp-java
mvn clean install
```

## 社区

- [贡献指南](CONTRIBUTING.md)
- [安全策略](SECURITY.md)
- [行为准则](CODE_OF_CONDUCT.md)
- [更新日志](CHANGELOG.md)

## 维护者

发布说明在 [docs/zh-CN/release.md](docs/zh-CN/release.md)。不要提交 Maven Central 凭据、GPG passphrase、私钥或 `settings.xml`。

## 许可证

[MIT](LICENSE)
