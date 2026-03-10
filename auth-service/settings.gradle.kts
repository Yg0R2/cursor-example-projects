rootProject.name = "auth-service"

val coreCatalogVersion: String by settings

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("coreLibs") {
            from("com.example.core:version-catalog:$coreCatalogVersion")
        }
    }
}

include("user-service-client")
include("api")
include("client")
include("service")
include("web")
include("application")
