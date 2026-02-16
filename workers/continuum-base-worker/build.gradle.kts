plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.google.cloud.tools.jib") version "3.4.0"
    `maven-publish`
}

group = "com.continuum.app.worker.base"
version = "1.0.0"
description = "Continuum Base Worker"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    // Springboot dependencies
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Kotlin dependencies
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Worker dependencies
    implementation(project(":continuum-springboot-starter-worker"))

    // Node dependencies
    implementation(project(":continuum-base"))

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.0")
        mavenBom("io.temporal:temporal-bom:1.28.0")
        mavenBom("software.amazon.awssdk:bom:2.30.7")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jib {
    from {
        image = "eclipse-temurin:21.0.8_9-jre"
    }
    to {
        val githubOwner = System.getenv("GITHUB_OWNER")
            ?: System.getenv("GITHUB_REPOSITORY")?.split("/")?.getOrNull(0)
            ?: "OWNER"
        image = "ghcr.io/${githubOwner}/${project.name}:${project.version}"
        auth {
            username = System.getenv("GITHUB_ACTOR") ?: System.getenv("MAVEN_REPO_USR")
            password = System.getenv("GITHUB_TOKEN") ?: System.getenv("MAVEN_REPO_PSW")
        }
    }
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
