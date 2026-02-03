plugins {
    id("core-library.kotlin-conventions")
}

// Core Client module - framework-agnostic client interfaces
dependencies {
    api(project(":core-api"))
}
