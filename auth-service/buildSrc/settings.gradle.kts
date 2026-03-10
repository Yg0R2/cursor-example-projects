val rootProperties = java.util.Properties()
val rootGradleProperties = java.io.File(settings.rootProject.projectDir.parentFile, "gradle.properties")
rootGradleProperties.inputStream().use { rootProperties.load(it) }
val coreCatalogVersion: String = rootProperties.getProperty("coreCatalogVersion")
    ?: error("coreCatalogVersion not found in root gradle.properties")

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
        create("coreLibs") {
            from("com.example.core:version-catalog:$coreCatalogVersion")
        }
    }
}
