plugins {
    id("service-template.spring-module-conventions")
}

dependencies {
    api(platform(coreLibs.core.platform))
    api(coreLibs.core.persistence)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}
