plugins {
    id("example.spring-module-conventions")
    alias(coreLibs.plugins.spring.boot)
}

dependencies {
    api(platform(coreLibs.core.platform))
    api(coreLibs.core.application)
    api(project(":web"))
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
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
