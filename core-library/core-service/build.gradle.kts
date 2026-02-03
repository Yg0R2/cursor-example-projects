plugins {
    id("core-library.kotlin-conventions")
}

// Core Service module - framework-agnostic business logic interfaces
dependencies {
    api(project(":core-api"))
    api(project(":core-persistence"))
}
