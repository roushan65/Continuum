import com.github.gradle.node.yarn.task.YarnTask

plugins {
    id("com.github.node-gradle.node") version "3.2.1"
}

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

tasks.register<YarnTask>("run") {
    args.set(listOf("run", "start:workbench"))
}

tasks.register("publish") {
  description = "Publish the built application to JFrog Artifactory"
  group = "Publishing tasks"
  // don't publish yet
}

tasks.register<Exec>("jib") {
  description = "Docker build and push to JFrog Artifactory"
  group = "Jib tasks"
  commandLine("bash", "-c",
    "docker build -t elilillyco-continuum-docker-lc.jfrog.io/continuum-workbench:$version . --progress=plain && " +
        "docker login elilillyco-continuum-docker-lc.jfrog.io --username ${System.getenv("MAVEN_REPO_USERNAME")} --password ${System.getenv("MAVEN_REPO_PASSWORD")} && " +
        "docker push elilillyco-continuum-docker-lc.jfrog.io/continuum-workbench:$version"
  )
}