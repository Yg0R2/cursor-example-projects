plugins {
    id("example.spring-module-conventions")
    id("org.jetbrains.kotlin.plugin.spring")
}

dependencies {
    api(platform(coreLibs.core.platform))
    api(coreLibs.core.web)
    api(project(":service"))
    api(project(":user-service-client"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
}
