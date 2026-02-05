---
name: Java 25 Migration
overview: Migrate the core-library project from Java 21 to Java 25 by updating the Kotlin version to 2.3.10 and adjusting Java toolchain and JVM target settings.
todos:
  - id: update-kotlin-version
    content: Update Kotlin version from 2.1.0 to 2.3.10 in `libs.versions.toml`
    status: completed
  - id: update-java-toolchain
    content: Update Java toolchain from 21 to 25 in `core-library.kotlin-conventions.gradle.kts`
    status: completed
  - id: update-jvm-target
    content: Update Kotlin JVM target from JVM_21 to JVM_25 in `core-library.kotlin-conventions.gradle.kts`
    status: completed
  - id: verify-build
    content: Run `./gradlew build` to verify the migration
    status: completed
isProject: false
---

# Java 25 Migration Plan

## Summary

The migration requires changes to **2 files**:

- `[core-library/gradle/libs.versions.toml](core-library/gradle/libs.versions.toml)` - Kotlin version
- `[core-library/buildSrc/src/main/kotlin/core-library.kotlin-conventions.gradle.kts](core-library/buildSrc/src/main/kotlin/core-library.kotlin-conventions.gradle.kts)` - Java toolchain and JVM target

## Current Configuration


| Setting           | Current Value | New Value |
| ----------------- | ------------- | --------- |
| Kotlin version    | 2.1.0         | 2.3.10    |
| Java toolchain    | 21            | 25        |
| Kotlin JVM target | JVM_21        | JVM_25    |


## Changes

### 1. Update Kotlin Version in Version Catalog

In `[core-library/gradle/libs.versions.toml](core-library/gradle/libs.versions.toml)`:

```toml
[versions]
spring-boot = "4.0.2"
kotlin = "2.3.10"
```

This version is automatically used by:

- `buildSrc/build.gradle.kts` for the Kotlin Gradle plugin dependency
- All submodules via the `kotlin-jvm` plugin alias

### 2. Update Java Toolchain and Kotlin JVM Target

In `[core-library/buildSrc/src/main/kotlin/core-library.kotlin-conventions.gradle.kts](core-library/buildSrc/src/main/kotlin/core-library.kotlin-conventions.gradle.kts)`:

```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
    }
}
```

## Prerequisites

Before running the build, ensure:

- Java 25 JDK is installed and available
- Gradle can locate the JDK via toolchain auto-detection or `JAVA_HOME`

## Verification

After making the changes, run:

```bash
./gradlew build
```

