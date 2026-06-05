# 安全说明

中文 | [English](../security.md)

本项目面向本地、用户授权的 MCP 访问场景，用于连接开发者可控的服务。

## 连接 Agent 到数据前

在把 Redis、MySQL 或你自己的工具连接到真实数据前，先检查：

- 先使用本地或一次性测试数据
- 使用最小权限凭据
- 首次运行避免连接生产数据
- 凭据只放在本地 Agent 配置或环境变量中
- 使用高权限账号前先审查生成的 SQL 或工具动作

## 敏感信息

- 不要提交数据库密码、Redis 密码、API Key、Maven Central 凭据、GPG passphrase、私钥或 `settings.xml`。
- 运行时凭据应放在本地 Agent 配置或环境变量中。
- Demo 和集成测试建议使用独立测试凭据。

## Stdio 传输

- stdout 保留给 MCP JSON-RPC 协议消息。
- 服务日志通过 `logback.xml` 写入 stderr。
- 不要在 stdio server 中添加 `System.out.println` 日志。

## MySQL 安全

MySQL Server 会拒绝：

- 多语句 SQL
- `query` 中的非 `SELECT` 语句
- `INSERT` / `UPDATE` 之外的写语句
- `DELETE`、`DROP`、`ALTER`、`TRUNCATE`、`GRANT`、`REVOKE` 等危险关键字

建议的生产姿态：

- 为 MCP 访问创建专用 MySQL 用户
- 只授予 Agent 所需的数据库和操作权限
- 除非确实需要写入，否则优先使用只读凭据
- 先连接非关键或测试数据库验证

## Redis 安全

Redis Server 限制高风险访问模式：

- 默认禁用 `del`
- 拒绝 `*` 等宽泛 key 扫描
- 限制 key、field、TTL 和集合读取大小
- Redis `INFO` 限制为安全诊断 section

建议的生产姿态：

- 使用专用 Redis 数据库或命名空间前缀
- 未经评审不要让 Agent 访问共享生产 key
- Prompt 中优先指定命名空间，例如 `demo:*` 或 `cache:profile:*`

## 自定义 Server 模板安全

向 `mcp-examples/custom-server-template` 或你自己的 Server 添加工具时：

- 在系统边界校验工具输入
- 除非命令和参数被严格约束，否则避免在工具处理器中执行 shell 命令
- 不要记录密钥或完整凭据
- stdio Server 保持 stdout 只输出协议消息
- 工具描述尽量窄且明确，方便 Agent 正确选择工具

## Agent 使用

Agent 会根据工具名称、工具描述和用户 Prompt 选择工具。涉及数据操作时，Prompt 应尽量明确：

```text
Use the mysql MCP server. Insert 3 demo rows into the users table in the mcp_demo database, then query them back.
```

在敏感环境使用高权限凭据前，建议先审查模型生成的 SQL。
