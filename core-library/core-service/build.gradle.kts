plugins {
    id("example.spring-module-conventions")
}

// Core Service module - framework-agnostic business logic interfaces
dependencies {
    api(project(":core-api"))
    api(project(":core-persistence"))
}
