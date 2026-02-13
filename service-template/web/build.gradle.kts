plugins {
    id("service-template.spring-module-conventions")
}

dependencies {
    api(platform(libs.core.platform))
    api(libs.core.web)
    api(project(":service"))
    implementation("org.springframework.boot:spring-boot-starter-web")
}
