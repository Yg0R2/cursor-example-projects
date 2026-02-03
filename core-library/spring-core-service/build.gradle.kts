plugins {
    id("core-library.spring-module-conventions")
}

// Spring Core Service module - Spring service implementations
dependencies {
    api(platform(project(":spring-core-platform")))
    api(project(":core-service"))
    api(project(":spring-core-persistence"))

    implementation(libs.spring.boot.starter)
}
