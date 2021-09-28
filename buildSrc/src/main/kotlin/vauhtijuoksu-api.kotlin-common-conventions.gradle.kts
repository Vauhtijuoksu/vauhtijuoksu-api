plugins {
    base
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm")
    id("com.diffplug.spotless")
    id("io.gitlab.arturbosch.detekt")
}

repositories {
    mavenCentral()
}

dependencies {
    constraints {
        val vertxVersion = "4.1.4"
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk16")
        // Codegen components
        implementation("io.swagger.codegen.v3:swagger-codegen-cli:3.0.26")
        implementation("org.webjars:swagger-ui:3.10.0")

        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.+")
        implementation("io.vertx:vertx-core:$vertxVersion")
        implementation("io.vertx:vertx-web:$vertxVersion")
        implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")

        testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
        testImplementation("io.vertx:vertx-junit5:$vertxVersion")
        testImplementation("io.vertx:vertx-web-client:$vertxVersion")
    }

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use JUnit Jupiter API for testing.
    testImplementation("org.junit.jupiter:junit-jupiter-api")

    // Use JUnit Jupiter Engine for testing.
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "16"
        }
    }

    // Remove timestamps and always use same file order in jars to make builds reproducible
    withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    test {
        useJUnitPlatform()
    }
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
