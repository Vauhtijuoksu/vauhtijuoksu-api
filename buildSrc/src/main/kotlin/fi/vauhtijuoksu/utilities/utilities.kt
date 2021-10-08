package fi.vauhtijuoksu.utilities

import org.gradle.process.ExecSpec

/**
 * Run a bash command with sensible defaults
 */
fun ExecSpec.bashCommand(cmd: String){
    commandLine("bash", "-euxo", "pipefail", "-c", cmd)
}
