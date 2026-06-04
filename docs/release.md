# Release Guide

[中文](zh-CN/release.md) | English

This guide prepares the first public release, `0.1.0`.

## Release scope

Maven Central first-release focus:

- `com.mcp:mcp-sdk:0.1.0`
- `com.mcp:mcp-spring-boot-starter:0.1.0`
- common modules required by the MySQL and Redis servers

Executable server distribution focus:

- `mcp-server-mysql-0.1.0.jar`
- `mcp-server-redis-0.1.0.jar`

Other server modules skip Maven deploy for now to avoid publishing less-polished modules in the first release.

## Maven Central prerequisites

1. The `com.mcp` namespace is verified in your Central/Sonatype account.
2. Local `~/.m2/settings.xml` contains the `ossrh` server credentials.
3. A local GPG key is available for signing.
4. Maven Central credentials, GPG passphrases, private keys, and `settings.xml` are not committed to Git.

## Local verification

```bash
mvn -pl mcp-sdk,mcp-spring-boot-starter,mcp-server-collection/mcp-server-mysql,mcp-server-collection/mcp-server-redis -am verify -DskipTests
```

## Release command

Run this only after the account, signing key, and release scope are confirmed:

```bash
mvn clean deploy -Prelease -DskipTests
```

Run deploy or upload GitHub Release assets only after a separate explicit confirmation.

## GitHub Release assets

Recommended first-release assets:

- `mcp-server-collection/mcp-server-mysql/target/mcp-server-mysql-0.1.0.jar`
- `mcp-server-collection/mcp-server-redis/target/mcp-server-redis-0.1.0.jar`

Suggested release notes:

- SDK dependency coordinates
- MySQL/Redis MCP Server download and configuration examples
- 5-minute Quick Start link
- Security guide link
