plugins {
    id("service-template.spring-module-conventions")
}

dependencies {
    api(platform(libs.core.platform))
    api(libs.core.persistence)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}
