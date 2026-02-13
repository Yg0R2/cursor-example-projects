plugins {
    id("service-template.spring-module-conventions")
}

dependencies {
    api(platform(libs.core.platform))
    api(libs.core.service)
    api(project(":api"))
    api(project(":persistence"))
    implementation("org.springframework.boot:spring-boot-starter")
}
