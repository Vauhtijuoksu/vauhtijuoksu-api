/**
 * Configuration for all projects containing Kotlin code, including interfaces and test utils
 */

plugins {
    id("vauhtijuoksu-api.common-conventions")
    id("io.gitlab.arturbosch.detekt")
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.20.0")

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    // Use JUnit Jupiter API for testing.
    libs.findLibrary("junit-jupiter-api").ifPresentOrElse({
        testImplementation(it)
    }, {
        throw RuntimeException("No junit jupiter api")
    })

    // Use JUnit Jupiter Engine for testing.
    libs.findLibrary("junit-jupiter-engine").ifPresentOrElse({
        testRuntimeOnly(it)
    }, {
        throw RuntimeException("No junit jupiter engine")
    })
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    // Remove timestamps and always use same file order in jars to make builds reproducible
    withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}
