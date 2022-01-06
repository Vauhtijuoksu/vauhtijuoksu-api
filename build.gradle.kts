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

val versionOutput = ByteArrayOutputStream()
exec {
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

    val jacocoRootReport by registering(JacocoReport::class) {
        subprojects.filter {
            // feature-tests have a separate report
            it.name != "feature-tests"
        }.forEach { subproject ->
            // Depend on the subproject report so that those are generated those before the combined report
            subproject.tasks.findByName("jacocoTestReport")?.let { task ->
                dependsOn(task)
            }
            subproject.the<SourceSetContainer>().findByName("main")?.let {
                sourceSets(it)
            }
            plugins.withType<JacocoPlugin>().configureEach {
                subproject.tasks.matching {
                    it.extensions.findByType<JacocoTaskExtension>() != null
                }.configureEach {
                    executionData(this)
                }
            }
        }
        reports {
            xml.required.set(true)
            html.required.set(true)
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
                subproject.the<SourceSetContainer>().findByName("main")?.let {
                    sourceSets(it)
                }
            }
        }
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    build {
        finalizedBy(jacocoRootReport, featureTestReport)
    }
}
