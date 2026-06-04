---
name: MCP client compatibility issue
about: Report a compatibility problem with Claude Code, Codex, opencode, OpenClaw, Gemini-related tools, or another MCP client
title: "[Compatibility] "
labels: compatibility
assignees: ""
---

## MCP client

- Client name:
- Client version:
- OS:
- Server: `mcp-server-mysql` / `mcp-server-redis` / custom SDK server
- Version or commit:

## Configuration

```json
Paste the MCP server configuration shape here. Remove passwords, tokens, and local secrets.
```

## Failure point

Select the stage that fails:

- [ ] Server process does not start
- [ ] `initialize` handshake fails
- [ ] `tools/list` fails
- [ ] `tools/call` fails
- [ ] Tool result parsing fails
- [ ] Other

## Error output

```text
Paste relevant client/server error output here. Do not include secrets.
```

## Compatibility checks

- [ ] The config includes `"type": "stdio"` if the client requires it.
- [ ] The server logs are written to stderr, not stdout.
- [ ] You are using a freshly rebuilt jar.
- [ ] Tool definitions include `inputSchema`.

## Additional context

Add anything else that may help reproduce the client behavior.
