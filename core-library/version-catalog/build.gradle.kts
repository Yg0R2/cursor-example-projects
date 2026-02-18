plugins {
    `version-catalog`
    `maven-publish`
}

group = rootProject.group
version = rootProject.version

catalog {
    versionCatalog {
        from(files("../gradle/libs.versions.toml"))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["versionCatalog"])
            pom {
                name.set(project.name)
                description.set("Version catalog for core-library: Spring Boot, Kotlin, and core module versions.")
            }
        }
    }
    repositories {
        mavenLocal()
        if (!version.toString().endsWith("-SNAPSHOT")) {
            maven {
                name = "GitHubPackages"
                url = uri(project.rootProject.property("github.packages.url") as String)
                credentials {
                    username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String?
                    password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as String?
                }
            }
        }
    }
}
