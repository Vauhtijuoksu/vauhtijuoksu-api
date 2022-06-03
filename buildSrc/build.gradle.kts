plugins {
    // Support convention plugins written in Kotlin. Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
    `kotlin-dsl`
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
}

buildscript {
    repositories {
        mavenCentral()
    }
}

dependencies {
    implementation(libs.kotlin.plugin)
    implementation(libs.spotless.plugin)
    implementation(libs.detekt.plugin)
    implementation(libs.swagger.generator.plugin)

    // Used by feature tests to gather coverage
    implementation(libs.jacoco.core)
    implementation(libs.jacoco.agent)
    implementation(libs.kubernetes.client)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}
