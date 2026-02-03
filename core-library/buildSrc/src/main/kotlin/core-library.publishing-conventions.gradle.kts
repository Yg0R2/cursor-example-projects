plugins {
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            // Defer configuration until components are available
            afterEvaluate {
                // Auto-detect: use javaPlatform component if available, otherwise java
                val component = components.findByName("javaPlatform")
                    ?: components.findByName("java")
                
                component?.let { from(it) }
            }
            
            pom {
                name.set(project.name)
                description.set("Core library module: ${project.name}")
            }
        }
    }
    
    repositories {
        mavenLocal()
        
        // Only add GitHub Packages for non-SNAPSHOT versions
        if (!version.toString().endsWith("-SNAPSHOT")) {
            maven {
                name = "GitHubPackages"
                url = uri(property("github.packages.url") as String)
                credentials {
                    username = System.getenv("GITHUB_ACTOR") ?: findProperty("gpr.user") as String?
                    password = System.getenv("GITHUB_TOKEN") ?: findProperty("gpr.key") as String?
                }
            }
        }
    }
}
