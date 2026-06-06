# Custom MCP Server Template

A copyable Java MCP Server template using annotations and stdio transport.

Use this when you want to build your own MCP server instead of starting from MySQL or Redis examples.

## What it includes

- `hello(name)` tool for a simple text result
- `get_project_status(project?)` tool for a JSON result
- `template://server-info` resource
- `draft_release_note(feature, audience?)` prompt
- Runnable shaded jar with `CustomServerTemplateMain`
- Logback configured to write logs to stderr so stdout stays reserved for MCP JSON-RPC

## Build

From the repository root:

```bash
mvn package -pl mcp-examples/custom-server-template -am -DskipTests
```

The runnable jar is generated at:

```text
mcp-examples/custom-server-template/target/custom-server-template-0.1.1.jar
```

## Configure an MCP client

Use an absolute jar path in your Agent configuration:

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

Restart your Agent after changing the configuration.

## Try it

Example prompts:

```text
Use the custom-template MCP server to say hello to Alice.
```

```text
Use the custom-template MCP server to get project status for mcp-java.
```

```text
Use the custom-template MCP server to draft a release note for adding a new payment tool.
```

## Customize it

1. Rename the Maven artifact in `pom.xml`.
2. Rename the Java package and classes under `src/main/java`.
3. Update `@McpServer(name = "...")`.
4. Add your own `@McpTool`, `@McpResource`, and `@McpPrompt` methods.
5. Rebuild and update the jar path in your Agent configuration.

## Safety checklist

- Validate all external input in tool handlers.
- Keep secrets out of source code and logs.
- Do not print to stdout from stdio servers; use a logger so output goes to stderr.
- Be careful with file system, shell, database, and network operations exposed as tools.
