plugins {
    id("service-template.kotlin-conventions")
    alias(libs.plugins.spring.boot)
}

dependencies {
    api(platform(libs.core.platform))
    api(libs.core.application)
    api(project(":web"))
    implementation("org.springframework.boot:spring-boot-starter")
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>("bootBuildImage") {
    imageName.set("${project.group}/${rootProject.name}")
}
