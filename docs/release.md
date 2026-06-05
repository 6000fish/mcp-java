# Release Guide

[中文](zh-CN/release.md) | English

This guide prepares the first public release, `0.1.0`.

## Release scope

Maven Central first-release artifacts:

- `io.github.6000fish:mcp-java:0.1.0` parent POM
- `io.github.6000fish:mcp-sdk:0.1.0`
- `io.github.6000fish:mcp-spring-boot-starter:0.1.0`
- `io.github.6000fish:mcp-server-collection:0.1.0` parent POM
- `io.github.6000fish:mcp-server-common:0.1.0`
- `io.github.6000fish:mcp-server-mysql:0.1.0`
- `io.github.6000fish:mcp-server-redis:0.1.0`

Executable server distribution focus:

- `mcp-server-mysql-0.1.0.jar`
- `mcp-server-redis-0.1.0.jar`

Other server modules skip Maven deploy for now to avoid publishing less-polished modules in the first release.

## Maven Central prerequisites

1. The `io.github.6000fish` namespace is verified in your Central/Sonatype account.
2. Local `~/.m2/settings.xml` contains the `central` server credentials generated from Central Portal tokens.
3. A local GPG key is available for signing.
4. Maven Central credentials, GPG passphrases, private keys, and `settings.xml` are not committed to Git.

## Local verification

```bash
mvn -pl .,mcp-sdk,mcp-spring-boot-starter,mcp-server-collection,mcp-server-collection/mcp-server-common,mcp-server-collection/mcp-server-mysql,mcp-server-collection/mcp-server-redis verify -DskipTests
```

## Release command

Run this only after the account, signing key, and release scope are confirmed:

```bash
mvn clean deploy -Prelease -DskipTests -pl .,mcp-sdk,mcp-spring-boot-starter,mcp-server-collection,mcp-server-collection/mcp-server-common,mcp-server-collection/mcp-server-mysql,mcp-server-collection/mcp-server-redis
```

This uploads a Central Portal deployment with `autoPublish=false`; review the deployment in Central Portal before publishing it.

## GitHub Release assets

Recommended first-release assets:

- `mcp-server-collection/mcp-server-mysql/target/mcp-server-mysql-0.1.0.jar`
- `mcp-server-collection/mcp-server-redis/target/mcp-server-redis-0.1.0.jar`

Suggested release notes:

- SDK dependency coordinates
- MySQL/Redis MCP Server download and configuration examples
- 5-minute Quick Start link
- Security guide link
