plugins {
    id("core-library.kotlin-conventions")
}

// Core Web module - framework-agnostic controller interfaces
dependencies {
    api(project(":core-service"))
}
