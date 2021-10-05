import java.io.File
import java.io.FileInputStream
import java.io.IOException

plugins {
    id("vauhtijuoksu-api.kotlin-common-conventions")
    id("application")
}

dependencies {
    implementation(project(path = ":models"))
    implementation(project(path = ":database"))

    implementation("com.google.inject:guice")
    implementation("com.sksamuel.hoplite:hoplite-core")
    implementation("com.sksamuel.hoplite:hoplite-yaml")
    implementation("io.vertx:vertx-core")
    implementation("io.vertx:vertx-web")
    implementation("io.vertx:vertx-lang-kotlin")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("io.vertx:vertx-junit5")
    testImplementation("io.vertx:vertx-web-client")
    testImplementation("org.mockito:mockito-core")
}

application {
    mainClass.set("fi.vauhtijuoksu.vauhtijuoksuapi.server.ServerKt")
}

val dockerBuild by tasks.registering {
    description = "Build a docker image"
    val imageName = "vauhtijuoksu-api"
    val imageTag = "dev"
    inputs.files(tasks.distTar.get().path)
    inputs.file("Dockerfile")
    val imageHashFile = "$buildDir/image-hash"
    outputs.file(imageHashFile)
    outputs.upToDateWhen {
        val findHash = ProcessBuilder().command(
            "bash", "-c",
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
            commandLine("docker", "build", ".", "-t", "$imageName:$imageTag")
        }
        // Save the image hash so that gradle caches the step
        exec {
            commandLine(
                "bash", "-c",
                "docker image inspect $imageName:$imageTag | jq '.[0].Id' > build/image-hash"
            )
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
