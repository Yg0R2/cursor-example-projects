dependencyResolutionManagement {
    versionCatalogs {
        create("coreLibs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
