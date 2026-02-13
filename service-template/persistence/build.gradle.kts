plugins {
    id("service-template.spring-module-conventions")
}

dependencies {
    api(platform(libs.core.platform))
    api(libs.core.persistence)
    implementation(libs.spring.boot.starter.data.jpa)
}
