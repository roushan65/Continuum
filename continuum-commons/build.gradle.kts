plugins {
    kotlin("jvm") version "2.1.0"
    id("io.spring.dependency-management") version "1.1.6"
    `maven-publish`
}

group = "com.continuum.core"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Jackson dependencies
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")

    // Parquet writer
    implementation("org.apache.avro:avro:1.12.0")
    implementation("org.apache.parquet:parquet-avro:1.15.0")
    implementation("org.apache.hadoop:hadoop-common:3.4.1")
    implementation("org.apache.hadoop:hadoop-mapreduce-client-core:3.3.1")

    // Project dependencies
    implementation(project(":continuum-avro-schemas"))

    // Temporal dependencies
    implementation("io.temporal:temporal-sdk")
    implementation("io.temporal:temporal-kotlin")

    // Freemarker dependencies
    implementation("org.freemarker:freemarker:2.3.34")

    // JSON schema validation
    implementation("com.networknt:json-schema-validator:1.5.6")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.0")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.0")
        mavenBom("io.temporal:temporal-bom:1.28.0")
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
