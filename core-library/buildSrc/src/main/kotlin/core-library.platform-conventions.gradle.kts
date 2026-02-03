plugins {
    id("java-platform")
}

group = property("group") as String
version = property("version") as String

repositories {
    mavenCentral()
    mavenLocal()
}
