plugins {
    id("example.spring-module-conventions")
    id("org.jetbrains.kotlin.plugin.spring")
}

dependencies {
    api(platform(coreLibs.core.platform))
    api(coreLibs.core.service)
    api(project(":api"))
    api(project(":user-service-client"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-security")
}
