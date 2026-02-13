plugins {
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            afterEvaluate {
                val component = components.findByName("java")
                component?.let { from(it) }
            }
            pom {
                name.set(project.name)
                description.set("Service template module: ${project.name}")
            }
        }
    }
    repositories {
        mavenLocal()
        // Add Artifactory when configured (e.g. artifactory.url in gradle.properties)
        findProperty("artifactory.url")?.let { url ->
            maven {
                name = "Artifactory"
                setUrl(url)
                credentials {
                    username = findProperty("artifactory.user") as String? ?: ""
                    password = findProperty("artifactory.password") as String? ?: ""
                }
            }
        }
    }
}
