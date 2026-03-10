plugins {
    id("example.spring-module-conventions")
}

// Spring Core API module - Spring-specific API extensions
dependencies {
    api(platform(project(":spring-core-platform")))
    api(project(":core-api"))

    // Optional: Spring validation support
    api(libs.spring.boot.starter.validation)
}
