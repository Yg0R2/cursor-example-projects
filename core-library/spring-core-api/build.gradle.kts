// Spring Core API module - Spring-specific API extensions
dependencies {
    api(platform(project(":spring-core-platform")))
    api(project(":core-api"))

    // Optional: Spring validation support
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
