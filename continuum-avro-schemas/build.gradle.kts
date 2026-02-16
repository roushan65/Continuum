plugins {
    id("java-library")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    `maven-publish`
}

group = "com.continuum.core"
version = "1.0.0"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    api("org.apache.avro:avro:1.12.0")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            group = project.group
            description = project.description
            version = project.version.toString()
            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://github.com/EliLillyCo/SPE_continuum")
            }
        }
    }
    repositories {
        maven {
            name = "github"
            val repo = System.getenv("GITHUB_REPOSITORY") ?: "OWNER/REPO"
            val parts = repo.split("/")
            val owner = parts.getOrElse(0) { "OWNER" }
            val repository = parts.getOrElse(1) { project.name }
            url = uri("https://maven.pkg.github.com/${owner}/${repository}")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: System.getenv("MAVEN_REPO_USR")
                password = System.getenv("GITHUB_TOKEN") ?: System.getenv("MAVEN_REPO_PSW")
            }
        }
    }
}
