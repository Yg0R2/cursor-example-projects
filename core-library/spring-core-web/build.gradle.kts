plugins {
    id("core-library.spring-module-conventions")
}

// Spring Core Web module - Spring MVC controller implementations
dependencies {
    api(platform(project(":spring-core-platform")))
    api(project(":core-web"))
    api(project(":spring-core-service"))

    implementation(libs.spring.boot.starter.web)
}
