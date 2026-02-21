plugins {
    id("example.platform-conventions")
}

javaPlatform {
    allowDependencies()
}

dependencies {
    // Import Spring Boot BOM for version management
    api(platform(libs.spring.boot.dependencies))
}
