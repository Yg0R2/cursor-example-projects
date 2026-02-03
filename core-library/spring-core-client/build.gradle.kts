// Spring Core Client module - Spring WebClient/RestClient implementations
dependencies {
    api(platform(project(":spring-core-platform")))
    api(project(":core-client"))

    implementation("org.springframework.boot:spring-boot-starter-webflux")
}
