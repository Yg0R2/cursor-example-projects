plugins {
    id("example.spring-module-conventions")
}

dependencies {
    api(platform(coreLibs.core.platform))
    api(coreLibs.core.client)
    api(project(":api"))
    implementation("org.springframework.boot:spring-boot-starter-webflux")
}
