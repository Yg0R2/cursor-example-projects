plugins {
    id("example.spring-module-conventions")
}

dependencies {
    api(platform(coreLibs.core.platform))
    api(coreLibs.core.service)
    api(project(":api"))
    api(project(":persistence"))
    implementation("org.springframework.boot:spring-boot-starter")
}
