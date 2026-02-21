plugins {
    id("example.spring-module-conventions")
}

// Core Web module - framework-agnostic controller interfaces
dependencies {
    api(project(":core-service"))
}
