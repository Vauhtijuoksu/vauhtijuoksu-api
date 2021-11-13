plugins {
    base
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm")
    id("com.diffplug.spotless")
}

repositories {
    mavenCentral()
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    val ktLintVersion = "0.42.1"
    kotlin {
        ktlint(ktLintVersion)
    }
    kotlinGradle {
        ktlint(ktLintVersion)
    }
}
