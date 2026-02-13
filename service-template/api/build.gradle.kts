plugins {
    id("service-template.spring-module-conventions")
}

dependencies {
    api(platform(libs.core.platform))
    api(libs.core.api)
    implementation(libs.spring.boot.starter.validation)
}
