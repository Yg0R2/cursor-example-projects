// Spring Core Web module - Spring MVC controller implementations
dependencies {
    api(platform(project(":spring-core-platform")))
    api(project(":core-web"))
    api(project(":spring-core-service"))

    implementation("org.springframework.boot:spring-boot-starter-web")
}
