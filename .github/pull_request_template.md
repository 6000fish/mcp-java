## Summary

- 

## Scope

- [ ] Server-side SDK
- [ ] MySQL MCP Server
- [ ] Redis MCP Server
- [ ] Spring Boot integration
- [ ] Documentation
- [ ] Compatibility
- [ ] Build/release infrastructure

## Validation

- [ ] `mvn -pl mcp-sdk,mcp-spring-boot-starter,mcp-server-collection/mcp-server-mysql,mcp-server-collection/mcp-server-redis -am test`
- [ ] `mvn package -pl mcp-server-collection/mcp-server-mysql,mcp-server-collection/mcp-server-redis -am -DskipTests`
- [ ] Manual MCP client validation, if compatibility behavior changed
- [ ] Documentation updated, if user-facing behavior changed

## Stdio compatibility checklist

- [ ] stdout only contains MCP JSON-RPC protocol messages
- [ ] logs are written to stderr
- [ ] tool definitions include `inputSchema`
- [ ] client extension fields such as `_meta` are tolerated
- [ ] stdio responses omit explicit null fields

## Security checklist

- [ ] No credentials, tokens, private keys, `.env` files, or local Agent configs are committed
- [ ] Database/cache destructive operations remain guarded or disabled by default
- [ ] User input at system boundaries is validated
