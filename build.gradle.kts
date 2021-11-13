import fi.vauhtijuoksu.utilities.bashCommand
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

plugins {
    id("vauhtijuoksu-api.kotlin-conventions")
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
        subprojects {
            // test-data is a special case, as it has code but no tests
            if (this@subprojects.name == "test-data") {
                return@subprojects
            }
            // Depend on the subproject report so that those are generated those before the combined report
            this@subprojects.tasks.findByName("jacocoTestReport")?.let {
                dependsOn(it)
            }
            plugins.withType<JacocoPlugin>().configureEach {
                tasks.matching {
                    it.extensions.findByType<JacocoTaskExtension>() != null
                }.configureEach {
                    this@subprojects.the<SourceSetContainer>().findByName("main")?.let {
                        sourceSets(it)
                        executionData(this)
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
        finalizedBy(jacocoRootReport)
    }
}
