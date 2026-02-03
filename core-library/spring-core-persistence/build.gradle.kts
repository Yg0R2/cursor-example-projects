// Spring Core Persistence module - Spring Data JPA implementations
dependencies {
    api(platform(project(":spring-core-platform")))
    api(project(":core-persistence"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}
