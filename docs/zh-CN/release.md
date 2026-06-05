# 发布与分发手册

中文 | [English](../release.md)

本手册用于首个公开版本 `0.1.0` 的发布准备。

## 发布范围

Maven Central 首发产物：

- `io.github.6000fish:mcp-java:0.1.0` parent POM
- `io.github.6000fish:mcp-sdk:0.1.0`
- `io.github.6000fish:mcp-spring-boot-starter:0.1.0`
- `io.github.6000fish:mcp-server-collection:0.1.0` parent POM
- `io.github.6000fish:mcp-server-common:0.1.0`
- `io.github.6000fish:mcp-server-mysql:0.1.0`
- `io.github.6000fish:mcp-server-redis:0.1.0`

可执行 Server 分发重点：

- `mcp-server-mysql-0.1.0.jar`
- `mcp-server-redis-0.1.0.jar`

其他 Server 模块暂时跳过 Maven deploy，避免首发发布未充分打磨的模块。

## Maven Central 前置条件

1. Central/Sonatype 账号已完成 `io.github.6000fish` namespace 校验。
2. 本地 `~/.m2/settings.xml` 配置从 Central Portal token 生成的 `central` server 凭据。
3. 本地 GPG key 可用于签名。
4. 不把 Maven Central 凭据、GPG passphrase、私钥或 `settings.xml` 提交到 Git。

## 本地验证

```bash
mvn -pl .,mcp-sdk,mcp-spring-boot-starter,mcp-server-collection,mcp-server-collection/mcp-server-common,mcp-server-collection/mcp-server-mysql,mcp-server-collection/mcp-server-redis verify -DskipTests
```

## 发布命令

仅在确认账号、签名和发布范围都无误后执行：

```bash
mvn clean deploy -Prelease -DskipTests -pl .,mcp-sdk,mcp-spring-boot-starter,mcp-server-collection,mcp-server-collection/mcp-server-common,mcp-server-collection/mcp-server-mysql,mcp-server-collection/mcp-server-redis
```

该命令会以 `autoPublish=false` 上传 Central Portal deployment；请先在 Central Portal 检查 deployment，再手动发布。

## GitHub Release 资产

建议首发上传：

- `mcp-server-collection/mcp-server-mysql/target/mcp-server-mysql-0.1.0.jar`
- `mcp-server-collection/mcp-server-redis/target/mcp-server-redis-0.1.0.jar`

Release notes 建议包含：

- SDK dependency 坐标
- MySQL/Redis MCP Server 下载与配置示例
- 5 分钟 Quick Start 链接
- 安全说明链接
