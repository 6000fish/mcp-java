# Contributing Guide

[中文](#中文) | English

Thank you for contributing to MCP Java SDK. This project focuses on server-side MCP SDK capabilities and ready-to-use MCP Servers.

## Scope

Welcome contributions in these areas:

- MCP server protocol compatibility
- MySQL and Redis MCP Server usability and safety
- Spring Boot integration
- Documentation and quick-start improvements
- Tests for mainstream MCP clients and stdio behavior

Client SDK work is not part of the current project direction.

## Development setup

Prerequisites:

- Java 17+
- Maven 3.9+

Run core tests:

```bash
mvn -pl mcp-sdk,mcp-spring-boot-starter,mcp-server-collection/mcp-server-mysql,mcp-server-collection/mcp-server-redis -am test
```

Package ready-to-use servers:

```bash
mvn package -pl mcp-server-collection/mcp-server-mysql,mcp-server-collection/mcp-server-redis -am -DskipTests
```

## Compatibility rules

For stdio MCP Servers:

- stdout must contain only MCP JSON-RPC messages.
- logs must be written to stderr.
- tool definitions must include `inputSchema`.
- request parsing should tolerate client extension fields such as `_meta`.
- stdio responses should omit explicit null fields.

## Security rules

- Do not commit credentials, private keys, GPG passphrases, `.env` files, or local Agent configs.
- Keep destructive database/cache operations guarded or disabled by default.
- MySQL tools must reject dangerous SQL and multi-statement SQL.
- Redis tools must reject broad key scans and unsafe destructive operations.

## Pull request checklist

- [ ] The change matches the server-side project scope.
- [ ] Core tests pass.
- [ ] Stdio behavior keeps stdout clean.
- [ ] Documentation is updated when user-facing behavior changes.
- [ ] No secrets or local-only config are committed.

# 中文

感谢你参与 MCP Java SDK。本项目当前聚焦 Server 端 MCP SDK 能力和开箱即用 MCP Server。

## 贡献范围

欢迎贡献：

- MCP Server 协议兼容性
- MySQL 和 Redis MCP Server 的可用性与安全性
- Spring Boot 集成
- 文档和快速开始体验
- 主流 MCP 客户端和 stdio 行为测试

当前项目方向不包含 Client SDK。

## 开发环境

前置条件：

- Java 17+
- Maven 3.9+

运行核心测试：

```bash
mvn -pl mcp-sdk,mcp-spring-boot-starter,mcp-server-collection/mcp-server-mysql,mcp-server-collection/mcp-server-redis -am test
```

打包开箱即用 Server：

```bash
mvn package -pl mcp-server-collection/mcp-server-mysql,mcp-server-collection/mcp-server-redis -am -DskipTests
```

## 兼容性规则

对于 stdio MCP Server：

- stdout 必须只包含 MCP JSON-RPC 协议消息。
- 日志必须写入 stderr。
- 工具定义必须包含 `inputSchema`。
- 请求解析应兼容 `_meta` 等客户端扩展字段。
- stdio 响应应省略显式 null 字段。

## 安全规则

- 不提交凭据、私钥、GPG passphrase、`.env` 文件或本地 Agent 配置。
- 破坏性数据库/缓存操作应默认受限或禁用。
- MySQL 工具必须拒绝危险 SQL 和多语句 SQL。
- Redis 工具必须拒绝宽泛 key 扫描和不安全破坏性操作。

## PR 检查清单

- [ ] 变更符合 Server 端项目范围。
- [ ] 核心测试通过。
- [ ] stdio 行为保持 stdout 干净。
- [ ] 用户可见行为变化已更新文档。
- [ ] 未提交任何敏感信息或本地专用配置。
