import fi.vauhtijuoksu.utilities.findLibraryOrThrow

/**
 * Configuration for all projects containing Kotlin code, including interfaces and test utils
 */

plugins {
    id("vauhtijuoksu-api.common-conventions")
    id("io.gitlab.arturbosch.detekt")
}

dependencies {
    val libs: VersionCatalog = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

    detektPlugins(libs.findLibraryOrThrow("detekt-formatting"))

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")


    testImplementation(platform(libs.findLibraryOrThrow("junit-bom")))
    testImplementation(platform(libs.findLibraryOrThrow("mockito-bom")))
    testImplementation(platform(libs.findLibraryOrThrow("testcontainers-bom")))

    // Use JUnit Jupiter API for testing.
    testImplementation(libs.findLibraryOrThrow("junit-jupiter-api"))
    // Use JUnit Jupiter Engine for testing.
    testRuntimeOnly(libs.findLibraryOrThrow("junit-jupiter-engine"))
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "21"
        }
    }

    // Remove timestamps and always use same file order in jars to make builds reproducible
    withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}
