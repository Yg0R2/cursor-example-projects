plugins {
    id("example.spring-module-conventions")
}

dependencies {
    api(platform(coreLibs.core.platform))
    api(coreLibs.core.api)
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
