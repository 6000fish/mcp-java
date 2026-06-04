# Security Policy

[中文](#中文) | English

## Supported versions

The first public release line is `0.1.x`. Security reports should target the latest released version or the current `dev` branch.

## Reporting a vulnerability

Please do not open a public GitHub issue for sensitive security reports.

Report security issues privately through GitHub Security Advisories if available, or contact the maintainer through the repository owner profile.

Include:

- Affected module: `mcp-sdk`, `mcp-server-mysql`, `mcp-server-redis`, or another module
- Version or commit
- Reproduction steps
- Impact
- Suggested fix, if known

Do not include real credentials, private keys, production database dumps, or local Agent secrets in reports.

## Security expectations

This project treats the following areas as security-sensitive:

- stdio protocol isolation: stdout must only contain MCP JSON-RPC messages
- database safety controls for MySQL tools
- cache safety controls for Redis tools
- credential handling in docs, examples, and tests
- request parsing compatibility without unsafe command execution

## Disclosure

We will try to acknowledge valid reports promptly, investigate impact, and publish a fix before public disclosure when appropriate.

# 中文

## 支持版本

首个公开发布线为 `0.1.x`。安全报告应基于最新发布版本或当前 `dev` 分支。

## 报告漏洞

请不要用公开 GitHub issue 报告敏感安全问题。

如 GitHub Security Advisories 可用，请优先通过私有安全报告提交；也可以通过仓库 owner 个人资料中的联系方式联系维护者。

请包含：

- 受影响模块：`mcp-sdk`、`mcp-server-mysql`、`mcp-server-redis` 或其他模块
- 版本或 commit
- 复现步骤
- 影响范围
- 如已知，可提供修复建议

不要在报告中包含真实凭据、私钥、生产数据库 dump 或本地 Agent 密钥。

## 安全重点

本项目将以下方面视为安全敏感：

- stdio 协议隔离：stdout 必须只包含 MCP JSON-RPC 消息
- MySQL 工具的数据库安全控制
- Redis 工具的缓存安全控制
- 文档、示例和测试中的凭据处理
- 请求解析兼容性，且不引入不安全命令执行

## 披露

我们会尽量及时确认有效报告、评估影响，并在适当情况下先发布修复再公开披露。
