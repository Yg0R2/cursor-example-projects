plugins {
    id("core-library.spring-module-conventions")
}

// Spring Core Client module - Spring WebClient/RestClient implementations
dependencies {
    api(platform(project(":spring-core-platform")))
    api(project(":core-client"))

    implementation(libs.spring.boot.starter.webflux)
}
