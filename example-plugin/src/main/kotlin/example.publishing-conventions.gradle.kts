plugins {
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            afterEvaluate {
                val component = components.findByName("javaPlatform")
                    ?: components.findByName("java")
                component?.let { from(it) }
            }
            pom {
                name.set(project.name)
                description.set("${project.group} module: ${project.name}")
            }
        }
    }
    repositories {
        mavenLocal()

        if (!version.toString().endsWith("-SNAPSHOT")) {
            findProperty("publishing.repository.url")?.let { repoUrl ->
                maven {
                    name = findProperty("publishing.repository.name") as String? ?: "Remote"
                    url = uri(repoUrl)
                    credentials {
                        username = System.getenv("PUBLISH_USERNAME")
                            ?: findProperty("publishing.repository.username") as String?
                        password = System.getenv("PUBLISH_PASSWORD")
                            ?: findProperty("publishing.repository.password") as String?
                    }
                }
            }
        }
    }
}
