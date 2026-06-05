# 故障排查

中文 | [English](../troubleshooting.md)

## Agent 找不到 MCP Server

确认 Agent 配置使用的是 jar 绝对路径：

```json
"args": ["-jar", "/absolute/path/to/mcp-java/mcp-server-collection/mcp-server-redis/target/mcp-server-redis-0.1.0.jar"]
```

如果 jar 不存在，重新构建：

```bash
mvn package -pl mcp-server-collection/mcp-server-redis -am -DskipTests
```

## 找不到 java 命令

安装 Java 17+ 并验证：

```bash
java -version
```

## 找不到 mvn 命令

安装 Maven 3.9+ 并验证：

```bash
mvn -version
```

## Redis 连接失败

确认 Redis 正在运行且可连接：

```bash
redis-cli -h localhost -p 6379 ping
```

检查 Agent 配置中的环境变量：

- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`

## MySQL 连接失败

确认 MySQL 正在运行且凭据可用：

```bash
mysql -h localhost -P 3306 -u root -p
```

检查 Agent 配置中的环境变量：

- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`

## Agent 提示缺少 `type`

部分客户端要求 MCP 配置中显式声明传输类型。在 `command` 同级添加 `"type": "stdio"`：

```json
{
  "type": "stdio",
  "command": "java"
}
```

## 握手失败或重连失败

先确认使用的是最新重新构建的 jar，并且 Agent 配置指向该 jar。严格客户端可能会因为 server 把日志写到 stdout、工具定义缺少 `inputSchema`、stdio 响应包含显式 null 字段而握手失败。

内置 MySQL 和 Redis stdio server 已覆盖这些兼容性点：

- 工具定义包含 `inputSchema`
- 请求解析兼容 `_meta` 等客户端扩展字段
- stdio 响应省略 null 字段
- 日志写入 stderr

## 日志出现在 Agent 输出里

stdio 模式下 stdout 必须只包含 MCP JSON-RPC 消息，日志应写入 stderr。

确认 server 模块包含 `src/main/resources/logback.xml`，并配置：

```xml
<target>System.err</target>
```

避免在 stdio server 代码中添加 `System.out.println`。

## 工具调用被拒绝

这通常是预期的安全行为。

MySQL 会拒绝危险 SQL、多语句 SQL 和破坏性操作。Redis 会拒绝宽泛 key 扫描、大集合读取、不安全 TTL 和删除操作。

可以尝试更窄、更安全的 Prompt，例如：

```text
Query the first 10 rows from users.
```

```text
Find Redis keys under demo:* only.
```

## Maven 构建出现 shade 警告

构建可执行 shaded jar 时，可能出现重叠资源或 `module-info.class` 相关警告。只要最终显示 `BUILD SUCCESS`，这些警告通常不影响当前 server jar 使用。

## Maven 构建出现 Spring Boot 插件版本警告

Reactor 可能提示示例模块中的 `spring-boot-maven-plugin` 没有显式版本。该警告不阻塞当前 SDK 和 Server 发布范围构建，但建议在后续完整打磨版本中清理。
