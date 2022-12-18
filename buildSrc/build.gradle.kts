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
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.22")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.12.0")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.22.0")
    // Used by feature tests to gather coverage
    implementation("org.jacoco:org.jacoco.core:0.8.8")
    implementation("io.fabric8:kubernetes-client:6.3.1")

    implementation("gradle.plugin.org.hidetake:gradle-swagger-generator-plugin:2.19.2")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}
