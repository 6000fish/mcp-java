# Troubleshooting

[中文](zh-CN/troubleshooting.md) | English

## First-run checklist

Before debugging client behavior, verify the local basics:

```bash
java -version
mvn -version
mvn package -pl mcp-examples/quick-start -am -DskipTests
```

Confirm the configured jar exists, then restart the Agent after changing MCP configuration.

## Agent cannot find the MCP server or tools

Check that the Agent configuration uses an absolute jar path:

```json
"args": ["-jar", "/absolute/path/to/mcp-java/mcp-examples/quick-start/target/quick-start-0.1.0.jar"]
```

Rebuild the jar if it does not exist:

```bash
mvn package -pl mcp-examples/quick-start -am -DskipTests
```

If the server appears but no tools are listed, check that:

- the path points to the latest rebuilt jar
- the config includes `"type": "stdio"`
- the Agent was restarted after editing config
- no server code writes logs to stdout

## Java command is not found

Install Java 17+ and verify:

```bash
java -version
```

## Maven command is not found

Install Maven 3.9+ and verify:

```bash
mvn -version
```

## Redis connection fails

Verify Redis is running and reachable:

```bash
redis-cli -h localhost -p 6379 ping
```

Check these environment variables in the Agent configuration:

- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`

## MySQL connection fails

Verify MySQL is running and credentials work:

```bash
mysql -h localhost -P 3306 -u root -p
```

Check these environment variables in the Agent configuration:

- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`

## Agent reports missing `type`

Some clients require the transport type in the MCP configuration. Add `"type": "stdio"` next to `command`:

```json
{
  "type": "stdio",
  "command": "java"
}
```

## Handshake or reconnect fails

Use the latest rebuilt jar and check that the Agent config points to that jar. Strict clients may fail if the server writes logs to stdout, returns tool definitions without `inputSchema`, or includes explicit null fields in stdio responses.

The built-in MySQL and Redis stdio servers are tested for these compatibility points:

- tool definitions include `inputSchema`
- request parsing tolerates client extension fields such as `_meta`
- stdio responses omit null fields
- logs are written to stderr

## Server starts and waits with no output

This can be normal for stdio MCP servers. They wait for JSON-RPC messages from the Agent on stdin. Logs should appear on stderr only.

If you run a stdio server manually with `java -jar ...`, do not expect a web page or command prompt response.

## Logs appear in Agent output or JSON-RPC parse errors

In stdio mode, stdout must contain only MCP JSON-RPC messages. Use stderr for logs.

Check that the server module contains `src/main/resources/logback.xml` with:

```xml
<target>System.err</target>
```

Avoid adding `System.out.println` to stdio server code.

## Tool call is rejected

This is often expected safety behavior.

MySQL rejects dangerous SQL, multi-statement SQL, and destructive operations. Redis rejects broad key scans, large collection reads, unsafe TTL values, and deletion.

Try a narrower, safer prompt, for example:

```text
Query the first 10 rows from users.
```

```text
Find Redis keys under demo:* only.
```

## Maven Central dependency is not found

Confirm the dependency coordinates use the published namespace:

```xml
<groupId>io.github.6000fish</groupId>
```

Maven Central search indexing can lag behind artifact availability. If the artifact is visible in Central Portal but not in search, wait and retry with a clean local Maven cache for that artifact.

## Maven build shows shade warnings

Warnings about overlapping resources or `module-info.class` can appear while building executable shaded jars. They are usually harmless for these server jars when the build finishes with `BUILD SUCCESS`.

## Maven build shows Spring Boot plugin version warning

The reactor may warn that `spring-boot-maven-plugin` has no explicit version in the example module. This does not block the current release-scope server and SDK build, but should be cleaned up before a full polish release.
