# Security Guide

[中文](zh-CN/security.md) | English

This project is designed for local, user-authorized MCP access to developer-controlled services.

## Before connecting an Agent to data

Use this checklist before connecting Redis, MySQL, or your own tools to real data:

- test with local or disposable data first
- use least-privilege credentials
- avoid production data on the first run
- keep credentials in local Agent configuration or environment variables
- review generated SQL or tool actions before using privileged accounts

## Secrets

- Do not commit database passwords, Redis passwords, API keys, Maven Central credentials, GPG passphrases, private keys, or `settings.xml`.
- Put runtime credentials in local Agent configuration or environment variables.
- Use separate test credentials for demos and integration tests.

## Stdio transport

- stdout is reserved for MCP JSON-RPC protocol messages.
- Server logs are written to stderr through `logback.xml`.
- Do not add `System.out.println` logging to stdio servers.

## MySQL safety

The MySQL server rejects:

- multi-statement SQL
- non-`SELECT` statements in `query`
- write statements other than `INSERT` and `UPDATE`
- dangerous keywords including `DELETE`, `DROP`, `ALTER`, `TRUNCATE`, `GRANT`, and `REVOKE`

Recommended production posture:

- create a dedicated MySQL user for MCP access
- grant only the databases and operations the Agent needs
- prefer read-only credentials unless write access is required
- connect to non-critical or test databases first

## Redis safety

The Redis server limits risky access patterns:

- `del` is disabled by default
- broad key scans such as `*` are rejected
- key, field, TTL, and collection read sizes are bounded
- Redis `INFO` is limited to safe diagnostic sections

Recommended production posture:

- use a dedicated Redis database or namespace prefix
- avoid giving Agents access to shared production keys without review
- prefer namespaced prompts such as `demo:*` or `cache:profile:*`

## Custom server template safety

When adding tools to `mcp-examples/custom-server-template` or your own server:

- validate tool inputs at the system boundary
- avoid shelling out from tool handlers unless commands and arguments are tightly constrained
- do not log secrets or full credentials
- keep stdout protocol-only for stdio servers
- prefer narrow, explicit tool descriptions so Agents choose tools correctly

## Agent usage

Agents choose tools based on tool names, descriptions, and user prompts. Keep prompts explicit when touching data:

```text
Use the mysql MCP server. Insert 3 demo rows into the users table in the mcp_demo database, then query them back.
```

Review generated SQL before using privileged credentials in sensitive environments.
