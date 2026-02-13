plugins {
    id("service-template.spring-module-conventions")
}

dependencies {
    api(platform(libs.core.platform))
    api(libs.core.client)
    api(project(":api"))
    implementation("org.springframework.boot:spring-boot-starter-webflux")
}
