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
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.21")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.12.0")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.20.0")
    // Used by feature tests to gather coverage
    implementation("org.jacoco:org.jacoco.core:0.8.8")
    implementation("io.fabric8:kubernetes-client:5.12.4")

    // The plugin is not released with OAS3 support, even though it exists in master
    implementation(files("${projectDir}/libs/gradle-swagger-generator-plugin-SNAPSHOT.jar"))
    // Used by the plugin
    implementation("com.github.fge:json-schema-validator:2.2.6")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.3")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}
