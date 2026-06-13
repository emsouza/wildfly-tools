# WildFly Tools - Build Documentation

## Project Overview

This is a Maven/Tycho-based Eclipse plugin project for WildFly server adapters. All modules share a unified version (`4.5.0-SNAPSHOT`) defined in the parent POM.

## Prerequisites

- **Java 17+** (recommended)
- **Maven 3.8+**
- **Eclipse Target Platform** (configured via `target-platform` profile)

## Build Commands

### Full Build (all modules)

Tests run by default. Use `-DskipTests` to skip them (not recommended).

```bash
mvn clean install
```

### Build with Target Platform

```bash
mvn clean install -P target-platform
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
| `target-platform` | Uses JBoss Tools target platform from repository |
| `multiple.target` | Uses multiple target platforms |

## Project Structure

```
wildfly-tools/
├── pom.xml                    # Parent POM (version: 4.5.0-SNAPSHOT)
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
Missing requirement: ... requires 'org.eclipse.equinox.p2.iu; org.eclipse.jdt.ui 4.5.0.qualifier'
```

The target platform is not properly configured. Ensure:
1. Target platform profile is activated: `-P target-platform`
2. Network access to JBoss repositories
3. Correct Eclipse version in target platform definition

### Tycho Version

Tycho version: `5.0.3` (defined in parent POM property `tychoVersion`)

## IDE Import

1. Install **m2e** and **Tycho Configurator** in Eclipse
2. Import as **Existing Maven Projects**
3. Enable **Target Platform** via Preferences > Plug-in Development > Target Platform

## CI/CD

For automated builds, ensure:
- Maven settings.xml has JBoss repository credentials
- Target platform is available (cache or local mirror)
- Java 17+ is used

## Useful Commands

```bash
# Check effective version for all modules
mvn help:evaluate -Dexpression=project.version -q -DforceStdout

# Show dependency tree
mvn dependency:tree -pl plugins/base/org.jboss.tools.foundation.core

# Analyze plugin dependencies
mvn tycho:dependency-tree -pl features/org.jboss.tools.jmx.feature
```
