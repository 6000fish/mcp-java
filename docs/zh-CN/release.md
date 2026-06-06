# 发布与分发手册

中文 | [English](../release.md)

本手册记录当前发布状态，以及后续 `0.1.x` 版本的维护者发布流程。

## 候选发布：0.1.1

`v0.1.1` 是一次首发上手体验打磨候选版本：

- SDK 优先的 README 和 5 分钟快速开始。
- 可复制的注解式自定义 MCP Server 模板。
- 改进安全、故障排查和发布文档。
- CI 覆盖 onboarding 示例打包验证。

除非明确提升新模块，否则 Maven Central 发布范围保持与 `v0.1.0` 一致。

## 当前发布：0.1.0

首个公开版本已发布到 Maven Central 的产物：

- `io.github.6000fish:mcp-java:0.1.0` parent POM
- `io.github.6000fish:mcp-sdk:0.1.0`
- `io.github.6000fish:mcp-spring-boot-starter:0.1.0`
- `io.github.6000fish:mcp-server-collection:0.1.0` parent POM
- `io.github.6000fish:mcp-server-common:0.1.0`
- `io.github.6000fish:mcp-server-mysql:0.1.0`
- `io.github.6000fish:mcp-server-redis:0.1.0`

GitHub Release `v0.1.0` 资产：

- `mcp-server-mysql-0.1.0.jar`
- `mcp-server-redis-0.1.0.jar`

其他 Server 模块暂不纳入首个 Maven Central 发布范围。

## 发布后检查清单

发布后确认：

- Maven Central 能看到 `io.github.6000fish:mcp-sdk` 和 `io.github.6000fish:mcp-spring-boot-starter`。
- GitHub Release 存在，并包含预期的可执行 Server jar。
- README 中 Maven Central、Release、CI、Java、License badges 正常显示。
- 默认分支 CI 为绿色。
- [5 分钟快速开始](quickstart.md) 能从干净 clone 跑通。
- README 中能访问[安全说明](security.md)和[故障排查](troubleshooting.md)。

## 后续 0.1.x 发布范围

除非明确提升新模块，否则保持发布范围精确：

```bash
mvn -pl .,mcp-sdk,mcp-spring-boot-starter,mcp-server-collection,mcp-server-collection/mcp-server-common,mcp-server-collection/mcp-server-mysql,mcp-server-collection/mcp-server-redis verify -DskipTests
```

发布模块：

- root parent POM
- `mcp-sdk`
- `mcp-spring-boot-starter`
- `mcp-server-collection` parent POM
- `mcp-server-common`
- `mcp-server-mysql`
- `mcp-server-redis`

## Maven Central 前置条件

1. Central/Sonatype 账号已完成 `io.github.6000fish` namespace 校验。
2. 本地 `~/.m2/settings.xml` 配置从 Central Portal token 生成的 `central` server 凭据。
3. 本地 GPG key 可用于签名。
4. 不把 Maven Central 凭据、GPG passphrase、私钥或 `settings.xml` 提交到 Git。

## 上传到 Central Portal

仅在确认账号、签名、版本和发布范围都无误后执行。该命令会把选定模块上传到 Central Portal deployment；因为 `autoPublish=false`，不会自动发布。

```bash
mvn clean deploy -Prelease -DskipTests -pl .,mcp-sdk,mcp-spring-boot-starter,mcp-server-collection,mcp-server-collection/mcp-server-common,mcp-server-collection/mcp-server-mysql,mcp-server-collection/mcp-server-redis
```

请先在 Central Portal 检查 deployment，再手动发布。

## GitHub Release 资产

建议上传的可执行资产：

- `mcp-server-collection/mcp-server-mysql/target/mcp-server-mysql-<version>.jar`
- `mcp-server-collection/mcp-server-redis/target/mcp-server-redis-<version>.jar`

Release notes 建议包含：

- Maven Central 坐标
- MySQL/Redis MCP Server 下载与配置示例
- 5 分钟 Quick Start 链接
- 安全说明链接
- 故障排查链接

## v0.1.x 增长版本检查清单

发布 `v0.1.1` 这类体验打磨版本前，确认：

- README 链接和 badges 正常。
- `mcp-examples/quick-start` 能构建，且与 quickstart 文档一致。
- `mcp-examples/custom-server-template` 能构建，并且方便复制。
- MySQL 和 Redis Server 文档与实际工具、安全行为一致。
- CI 会打包 onboarding examples 和 ready-to-use servers。
