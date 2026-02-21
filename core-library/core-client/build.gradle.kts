plugins {
    id("example.spring-module-conventions")
}

// Core Client module - framework-agnostic client interfaces
dependencies {
    api(project(":core-api"))
}
