plugins {
    id("example.spring-module-conventions")
}

// Spring Core Web module - Spring MVC controller implementations
dependencies {
    api(platform(project(":spring-core-platform")))
    api(project(":core-web"))
    api(project(":spring-core-service"))

    api(libs.spring.boot.starter.web)
}
