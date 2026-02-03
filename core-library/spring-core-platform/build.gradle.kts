plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    // Import Spring Boot BOM for version management
    api(platform("org.springframework.boot:spring-boot-dependencies:4.0.2"))
}
