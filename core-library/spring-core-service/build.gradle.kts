// Spring Core Service module - Spring service implementations
dependencies {
    api(platform(project(":spring-core-platform")))
    api(project(":core-service"))
    api(project(":spring-core-persistence"))

    implementation("org.springframework.boot:spring-boot-starter")
}
