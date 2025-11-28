import fi.vauhtijuoksu.utilities.ExecOperationsProvider
import fi.vauhtijuoksu.utilities.bashCommand
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets


plugins {
    id("vauhtijuoksu-api.kotlin-conventions")
    jacoco
}

buildscript {
    repositories {
        // ktlint linter needs this for some reason
        mavenCentral()
    }
}

val execOperations = objects.newInstance(ExecOperationsProvider::class).execOperations
val versionOutput = ByteArrayOutputStream()
execOperations.exec {
    standardOutput = versionOutput
    workingDir = projectDir
    bashCommand("./scripts/version.sh")
}

version = versionOutput.toString(StandardCharsets.UTF_8).trim()

tasks {
    register("printVersion") {
        doLast {
            println(version)
        }
    }

    val featureTestReport by registering(JacocoReport::class) {
        subprojects.first { it.name == "feature-tests" }.let { featureTests ->
            featureTests.tasks.findByName("test")?.let {
                dependsOn(it)
            }
        }

        executionData.setFrom(fileTree("$projectDir/feature-tests/build/jacoco/").include("test-pod-*.exec"))

        subprojects.forEach { subproject ->
            plugins.withType<JacocoPlugin>().configureEach {
                subproject.tasks
                    .matching {
                        it.extensions.findByType<JacocoTaskExtension>() != null
                    }.configureEach {
                        subproject.the<SourceSetContainer>().findByName("main")?.let {
                            sourceSets(it)
                        }
                    }
            }
        }
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    build {
        finalizedBy(featureTestReport)
    }
}
