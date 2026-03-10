plugins {
    id("example.kotlin-conventions")
    id("org.jetbrains.kotlin.plugin.spring")
    alias(coreLibs.plugins.spring.boot)
}

dependencies {
    api(platform(coreLibs.core.platform))
    api(coreLibs.core.application)
    api(project(":web"))
    api(project(":user-service-client"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-security")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>("bootBuildImage") {
    imageName.set("${project.group}/${rootProject.name}")
}
