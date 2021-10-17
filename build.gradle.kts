import fi.vauhtijuoksu.utilities.bashCommand
import java.io.ByteArrayOutputStream

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

version = versionOutput.toString(java.nio.charset.StandardCharsets.UTF_8).trim()

tasks.register("printVersion") {
    doLast {
        println(version)
    }
}
