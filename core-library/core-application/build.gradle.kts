plugins {
    id("core-library.kotlin-conventions")
}

// Core Application module - framework-agnostic application interfaces
dependencies {
    api(project(":core-web"))
}
