package fi.vauhtijuoksu.utilities

import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.provider.Provider
import org.gradle.process.ExecOperations
import org.gradle.process.ExecSpec
import javax.inject.Inject

// https://docs.gradle.org/current/userguide/service_injection.html#execoperations
interface ExecOperationsProvider {
    @get:Inject
    val execOperations: ExecOperations
}

/**
 * Run a bash command with sensible defaults
 */
fun ExecSpec.bashCommand(cmd: String) {
    commandLine("bash", "-euxo", "pipefail", "-c", cmd)
}

/**
 * Find a library using [VersionCatalog.findLibrary] or throw an error
 */
fun VersionCatalog.findLibraryOrThrow(alias: String): Provider<MinimalExternalModuleDependency> {
    return findLibrary(alias).orElseThrow { RuntimeException("No library named $alias") }
}
