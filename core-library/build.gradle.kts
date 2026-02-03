plugins {
    kotlin("jvm") version embeddedKotlinVersion apply false
}

allprojects {
    group = "com.example.core"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

subprojects {
    // Skip platform projects - they use java-platform plugin instead
    if (!project.name.endsWith("-platform")) {
        apply(plugin = "org.jetbrains.kotlin.jvm")

        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            }
        }
    }
}
