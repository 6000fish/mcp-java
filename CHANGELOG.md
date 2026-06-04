# Changelog

[中文](#中文) | English

All notable changes to this project will be documented in this file.

## 0.1.0 - First public release

### Added

- Core server-side MCP SDK with stdio and SSE transports.
- Annotation-driven tool, resource, and prompt registration.
- Spring Boot starter for MCP server integration.
- Ready-to-use MySQL MCP Server.
- Ready-to-use Redis MCP Server.
- Bilingual user documentation and maintainer release materials.
- GitHub Actions CI for core test and server packaging verification.

### Compatibility

- Verified Claude Code with MySQL and Redis stdio servers.
- Verified Codex with MySQL stdio server, including `tools/list`, `tools/call`, and client `_meta` fields.
- Tool definitions include `inputSchema`.
- Stdio responses omit explicit null fields.
- Stdio server logs are written to stderr.

### Security

- MySQL dangerous SQL and multi-statement SQL are rejected.
- Redis broad key scans and destructive delete operations are guarded.
- Secrets are documented as local-only configuration.

# 中文

本文件记录项目的重要变更。

## 0.1.0 - 首个公开版本

### 新增

- Server 端 MCP SDK 核心能力，支持 stdio 和 SSE 传输。
- 基于注解的工具、资源和 Prompt 注册。
- Spring Boot MCP Server 集成 starter。
- 开箱即用 MySQL MCP Server。
- 开箱即用 Redis MCP Server。
- 中英文用户文档和维护者发布材料。
- GitHub Actions CI，用于核心测试和 Server 打包验证。

### 兼容性

- 已验证 Claude Code 可连接 MySQL 和 Redis stdio server。
- 已验证 Codex 可连接 MySQL stdio server，包括 `tools/list`、`tools/call` 和客户端 `_meta` 字段。
- 工具定义包含 `inputSchema`。
- stdio 响应省略显式 null 字段。
- stdio server 日志写入 stderr。

### 安全性

- MySQL 拒绝危险 SQL 和多语句 SQL。
- Redis 对宽泛 key 扫描和破坏性删除操作提供限制。
- 敏感信息仅作为本地配置记录。
