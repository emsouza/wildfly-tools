# WildFly Tools - Build Documentation

## Project Overview

This is a Maven/Tycho-based Eclipse plugin project for WildFly server adapters. All modules share a unified version (`4.5.0-SNAPSHOT`) defined in the parent POM.

## Prerequisites

- **Java 21+**
- **Maven 3.8+**
- **No external target platform artifact needed** — the target platform definition is included in the project at `targetplatform/wildfly-tools.target`

## Build Commands

### Full Build (all modules)

Tests run by default. Use `-DskipTests` to skip them (not recommended).

```bash
mvn clean install
```

### Build Specific Module

```bash
# Single plugin
mvn clean install -pl plugins/base/org.jboss.tools.foundation.core

# Single feature
mvn clean install -pl features/org.jboss.tools.jmx.feature

# Multiple modules
mvn clean install -pl plugins/base/org.jboss.tools.foundation.core,plugins/as/org.jboss.ide.eclipse.as.core

# Host + test fragment (both needed to compile and run tests)
mvn clean install -pl plugins/jmx/org.jboss.tools.jmx.jolokia,plugins/jmx/org.jboss.tools.jmx.jolokia.test
```

### Skip Tests

```bash
mvn clean install -DskipTests
```

### Offline Mode

```bash
mvn clean install -o
```

## Profiles

| Profile | Description |
|---------|-------------|
| `target-platform` | Uses the project's built-in target platform file (`targetplatform/wildfly-tools.target`). Active by default. |
| `no-target-platform` (`-Dno-target-platform`) | Skips explicit target platform; Tycho resolves from declared p2 repositories. |

The `target-platform` profile uses `targetplatform/wildfly-tools.target` which defines the Eclipse 2026-06 SimRel repository with all required features and Orbit bundles.

## Project Structure

```
wildfly-tools/
├── pom.xml                    # Parent POM (version: 4.5.0-SNAPSHOT)
├── targetplatform/            # Target platform definition
│   └── wildfly-tools.target   # Eclipse 2026-06 target platform
├── plugins/
│   ├── base/                  # Core foundation plugins
│   ├── as/                    # Application Server plugins
│   ├── archives/              # Archive handling plugins
│   └── jmx/                   # JMX monitoring plugins
├── features/                  # Eclipse feature definitions
└── site/                      # Update site (eclipse-repository)
```

## Version Management

All modules **inherit version from parent** (no explicit `<version>` in child POMs). Feature XML files use `4.5.0.qualifier` which maps to Maven's `4.5.0-SNAPSHOT`.

To update version:
```bash
mvn versions:set -DnewVersion=4.6.0-SNAPSHOT
```

## Common Issues

### Target Platform Resolution Errors

If you see errors like:
```
Missing requirement: ... requires 'org.eclipse.equinox.p2.iu; org.eclipse.jdt.ui ...'
```

The target platform is not properly configured. Ensure:
1. The `.target` file in `targetplatform/` is correct and the repository URLs are accessible
2. Network access to `https://download.eclipse.org/releases/2026-06/` and similar p2 repositories
3. The Eclipse version in the target platform matches the project requirements

### Corrupted Maven Cache

If you get errors about `content.xml` or unresolved artifacts:
```bash
rm -rf ~/.m2/repository
mvn clean install
```

### Tycho & Encoding

- **Tycho version:** `5.0.3` (defined in parent POM property `tychoVersion`)
- **Source encoding:** UTF-8 (configured via `project.build.sourceEncoding` in parent POM)
- **Java compliance:** 21 (configured via `maven.compiler.source`/`target`)

## IDE Import

1. Install **m2e** and **Tycho Configurator** in Eclipse
2. Import as **Existing Maven Projects**
3. Set the target platform to the project's `targetplatform/wildfly-tools.target` file via Preferences > Plug-in Development > Target Platform

## CI/CD

For automated builds, ensure:
- Network access to p2 repositories (Eclipse 2026-06, m2e, etc.)
- Java 21+ is used
- Maven 3.8+ is used

## Useful Commands

```bash
# Check effective version for all modules
mvn help:evaluate -Dexpression=project.version -q -DforceStdout

# Show dependency tree
mvn dependency:tree -pl plugins/base/org.jboss.tools.foundation.core

# Analyze plugin dependencies
mvn tycho:dependency-tree -pl features/org.jboss.tools.jmx.feature
```
