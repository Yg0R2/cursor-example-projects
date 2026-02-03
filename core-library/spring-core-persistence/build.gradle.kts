plugins {
    id("core-library.spring-module-conventions")
}

// Spring Core Persistence module - Spring Data JPA implementations
dependencies {
    api(platform(project(":spring-core-platform")))
    api(project(":core-persistence"))

    implementation(libs.spring.boot.starter.data.jpa)
}
