plugins {
    id("service-template.spring-module-conventions")
}

dependencies {
    api(platform(libs.core.platform))
    api(libs.core.service)
    api(project(":api"))
    api(project(":persistence"))
    implementation(libs.spring.boot.starter)
}
