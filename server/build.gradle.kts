import fi.vauhtijuoksu.utilities.bashCommand
import java.io.FileInputStream
import java.io.IOException

plugins {
    id("vauhtijuoksu-api.implementation-conventions")
    id("application")
    `jacoco-report-aggregation`
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(path = ":models"))
    implementation(project(path = ":database-api"))
    implementation(project(path = ":database"))

    implementation(libs.arrow.kt)
    implementation(libs.guice)
    implementation(libs.kotlinx.serialization)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.yaml)
    implementation(libs.mu.logging)
    implementation(libs.vertx.auth.htpasswd)
    implementation(libs.vertx.auth.oauth)
    implementation(libs.vertx.core)
    implementation(libs.vertx.lang.kotlin)
    implementation(libs.vertx.lang.kotlincoroutines)
    implementation(libs.vertx.web.core)
    implementation(libs.vertx.web.client)
    implementation(libs.vertx.session.redis)
    implementation(libs.jakarta.validation.api)

    runtimeOnly(libs.jackson.dataformat.yaml)
    runtimeOnly(libs.jackson.databind)
    runtimeOnly(libs.log4j.slf4j18.impl)

    testImplementation(project(path = ":test-data"))

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.vertx.junit5)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.wirewock)
    testImplementation(libs.mock.oauth)
}

application {
    mainClass.set("fi.vauhtijuoksu.vauhtijuoksuapi.server.ServerKt")
    applicationDefaultJvmArgs = listOf("-Dlog4j2.configurationFile=/logging/log4j2.yaml")
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
            "bash",
            "-euo",
            "pipefail",
            "-c",
            "docker image inspect $imageName:$imageTag | jq '.[0].Id'",
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

tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
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
