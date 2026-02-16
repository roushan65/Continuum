import com.github.gradle.node.yarn.task.YarnTask

plugins {
    id("com.github.node-gradle.node") version "3.2.1"
}

val version = "1.0.0"
val description = "Continuum Workbench"

node {
    version.set("22.12.0") // Specify the Node.js version
    yarnVersion.set("1.22.22") // Specify the Yarn version
    download.set(true) // Automatically download and install Node.js
}

tasks.register<YarnTask>("yarnInstall") {
    args.set(listOf("install"))
}

tasks.register<YarnTask>("build") {
    dependsOn("yarnInstall")
    args.set(listOf("run", "build"))
}

tasks.register<Delete>("clean") {
    delete("continuum-workbench/lib")
    delete("workflow-editor-extension/lib")
}

tasks.named("build") {
    dependsOn("clean")
}

// Register task for docker build
tasks.register<Exec>("jib") {
    commandLine("bash", "-c",
        // Build and push to GitHub Container Registry. Configure GITHUB_OWNER/GITHUB_ACTOR/GITHUB_TOKEN
        "docker build -t ghcr.io/${System.getenv("GITHUB_OWNER") ?: System.getenv("GITHUB_REPOSITORY")?.split('/')?.getOrNull(0) ?: 'OWNER'}/continuum-workbench:$version . --progress=plain && " +
        "echo ${'$'}{System.getenv("GITHUB_TOKEN") ?: System.getenv("MAVEN_REPO_PSW")} | docker login ghcr.io -u ${'$'}{System.getenv("GITHUB_ACTOR") ?: System.getenv("MAVEN_REPO_USR")} --password-stdin && " +
        "docker push ghcr.io/${System.getenv("GITHUB_OWNER") ?: System.getenv("GITHUB_REPOSITORY")?.split('/')?.getOrNull(0) ?: 'OWNER'}/continuum-workbench:$version"
    )
}
