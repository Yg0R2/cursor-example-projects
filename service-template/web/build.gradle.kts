plugins {
    id("example.spring-module-conventions")
}

dependencies {
    api(platform(coreLibs.core.platform))
    api(coreLibs.core.web)
    api(project(":service"))
    implementation("org.springframework.boot:spring-boot-starter-web")
}
