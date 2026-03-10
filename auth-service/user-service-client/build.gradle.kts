plugins {
    id("example.spring-module-conventions")
}

dependencies {
    api(platform(coreLibs.core.platform))
    api(coreLibs.core.api)
    api(coreLibs.core.client)
    api(coreLibs.core.persistence)
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
