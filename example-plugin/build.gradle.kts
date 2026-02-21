plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "com.example.gradle"
version = "0.0.1-SNAPSHOT"

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
}

publishing {
    repositories {
        mavenLocal()
    }
}
