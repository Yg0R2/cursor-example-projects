rootProject.name = "service-template"

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

include("api")
include("client")
include("persistence")
include("service")
include("web")
include("application")
