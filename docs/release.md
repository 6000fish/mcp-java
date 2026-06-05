# Release Guide

[中文](zh-CN/release.md) | English

This guide records the current release state and the maintainer procedure for future `0.1.x` releases.

## Current release: 0.1.0

Maven Central artifacts published for the first public release:

- `io.github.6000fish:mcp-java:0.1.0` parent POM
- `io.github.6000fish:mcp-sdk:0.1.0`
- `io.github.6000fish:mcp-spring-boot-starter:0.1.0`
- `io.github.6000fish:mcp-server-collection:0.1.0` parent POM
- `io.github.6000fish:mcp-server-common:0.1.0`
- `io.github.6000fish:mcp-server-mysql:0.1.0`
- `io.github.6000fish:mcp-server-redis:0.1.0`

GitHub Release assets for `v0.1.0`:

- `mcp-server-mysql-0.1.0.jar`
- `mcp-server-redis-0.1.0.jar`

Other server modules are intentionally not part of the first Maven Central publication scope.

## Post-release checklist

After publishing a release, verify:

- Maven Central shows `io.github.6000fish:mcp-sdk` and `io.github.6000fish:mcp-spring-boot-starter`.
- GitHub Release exists and contains the intended executable server jars.
- README Maven Central, Release, CI, Java, and License badges render correctly.
- CI is green on the default branch.
- [5-Minute Quick Start](quickstart.md) works from a clean clone.
- [Security Guide](security.md) and [Troubleshooting](troubleshooting.md) are reachable from README.

## Release scope for future 0.1.x builds

Keep the release scope explicit unless a module is intentionally promoted:

```bash
mvn -pl .,mcp-sdk,mcp-spring-boot-starter,mcp-server-collection,mcp-server-collection/mcp-server-common,mcp-server-collection/mcp-server-mysql,mcp-server-collection/mcp-server-redis verify -DskipTests
```

Published modules:

- root parent POM
- `mcp-sdk`
- `mcp-spring-boot-starter`
- `mcp-server-collection` parent POM
- `mcp-server-common`
- `mcp-server-mysql`
- `mcp-server-redis`

## Maven Central prerequisites

1. The `io.github.6000fish` namespace is verified in your Central/Sonatype account.
2. Local `~/.m2/settings.xml` contains the `central` server credentials generated from Central Portal tokens.
3. A local GPG key is available for signing.
4. Maven Central credentials, GPG passphrases, private keys, and `settings.xml` are not committed to Git.

## Upload to Central Portal

Run this only after the account, signing key, version, and release scope are confirmed. This uploads the selected modules to a Central Portal deployment; because `autoPublish=false`, it does not publish automatically.

```bash
mvn clean deploy -Prelease -DskipTests -pl .,mcp-sdk,mcp-spring-boot-starter,mcp-server-collection,mcp-server-collection/mcp-server-common,mcp-server-collection/mcp-server-mysql,mcp-server-collection/mcp-server-redis
```

Review the deployment in Central Portal, then manually publish it.

## GitHub Release assets

Recommended executable assets:

- `mcp-server-collection/mcp-server-mysql/target/mcp-server-mysql-<version>.jar`
- `mcp-server-collection/mcp-server-redis/target/mcp-server-redis-<version>.jar`

Release notes should include:

- Maven Central coordinates
- MySQL/Redis MCP Server download and configuration examples
- 5-minute Quick Start link
- Security guide link
- Troubleshooting link

## v0.1.x growth release checklist

Before cutting a polish release such as `v0.1.1`, verify:

- README links and badges work.
- `mcp-examples/quick-start` builds and matches the quickstart docs.
- `mcp-examples/custom-server-template` builds and is easy to copy.
- MySQL and Redis server docs match their actual tools and safety behavior.
- CI packages onboarding examples and ready-to-use servers.
