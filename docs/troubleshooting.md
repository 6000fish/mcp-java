# Troubleshooting

[中文](zh-CN/troubleshooting.md) | English

## Agent cannot find the MCP server

Check that the Agent configuration uses an absolute jar path:

```json
"args": ["-jar", "/absolute/path/to/mcp-server-redis-0.1.0.jar"]
```

Rebuild the jar if it does not exist:

```bash
mvn package -pl mcp-server-collection/mcp-server-redis -am -DskipTests
```

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

## Logs appear in Agent output

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

## Maven build shows shade warnings

Warnings about overlapping resources or `module-info.class` can appear while building executable shaded jars. They are usually harmless for these server jars when the build finishes with `BUILD SUCCESS`.

## Maven build shows Spring Boot plugin version warning

The reactor may warn that `spring-boot-maven-plugin` has no explicit version in the example module. This does not block the current release-scope server and SDK build, but should be cleaned up before a full polish release.
