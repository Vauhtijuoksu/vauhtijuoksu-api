import fi.vauhtijuoksu.utilities.bashCommand
import java.io.File
import java.io.FileInputStream
import java.io.IOException

plugins {
    id("vauhtijuoksu-api.implementation-conventions")
    id("application")
}

dependencies {
    implementation(project(path = ":models"))
    implementation(project(path = ":database-api"))
    implementation(project(path = ":database"))

    implementation("com.google.inject:guice")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.sksamuel.hoplite:hoplite-core")
    implementation("com.sksamuel.hoplite:hoplite-yaml")
    implementation("io.github.microutils:kotlin-logging-jvm")
    implementation("io.vertx:vertx-auth-htpasswd")
    implementation("io.vertx:vertx-core")
    implementation("io.vertx:vertx-web")
    implementation("io.vertx:vertx-lang-kotlin")

    runtimeOnly("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    runtimeOnly("com.fasterxml.jackson.core:jackson-databind")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j18-impl")

    testImplementation(project(path = ":test-data"))

    testImplementation("io.vertx:vertx-junit5")
    testImplementation("io.vertx:vertx-web-client")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}

application {
    mainClass.set("fi.vauhtijuoksu.vauhtijuoksuapi.server.ServerKt")
    applicationDefaultJvmArgs = listOf("-Dlog4j2.configurationFile=/configuration/log4j2.yaml")
}

val dockerBuild by tasks.registering {
    description = "Build a docker image"
    val imageName = "vauhtijuoksu/vauhtijuoksu-api"
    val imageTag = rootProject.version
    dependsOn(tasks.distTar)
    inputs.files(tasks.distTar.get().outputs.files)
    inputs.file("Dockerfile")
    val imageHashFile = "$buildDir/image-hash"
    outputs.file(imageHashFile)
    outputs.upToDateWhen {
        val findHash = ProcessBuilder().command(
            "bash", "-euo", "pipefail", "-c",
            "docker image inspect $imageName:$imageTag | jq '.[0].Id'"
        ).start()
        val foundImageHash = String(findHash.inputStream.readAllBytes()).trim()
        var previousHash: String? = null
        try {
            previousHash = String(FileInputStream(File(imageHashFile)).readAllBytes()).trim()
        } catch (e: IOException) {
            // No previous hash, needs rebuild
        }
        foundImageHash == previousHash
    }
    doLast {
        exec {
            bashCommand("docker build . -t $imageName:$imageTag")
        }
        // Save the image hash so that gradle caches the step
        exec {
            bashCommand("docker image inspect $imageName:$imageTag | jq '.[0].Id' > build/image-hash")
        }
    }
}

val dockerImageConfiguration: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add("dockerImageConfiguration", File("$buildDir/image-hash")) {
        builtBy(dockerBuild)
    }
}
