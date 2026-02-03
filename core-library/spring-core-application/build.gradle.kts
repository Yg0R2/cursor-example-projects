plugins {
    id("org.springframework.boot") version "4.0.2"
}

// Spring Core Application module - Spring Boot application bootstrap
// This is a library module, not an executable application
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}

dependencies {
    api(platform(project(":spring-core-platform")))
    api(project(":core-application"))
    api(project(":spring-core-web"))

    implementation("org.springframework.boot:spring-boot-starter")
}
