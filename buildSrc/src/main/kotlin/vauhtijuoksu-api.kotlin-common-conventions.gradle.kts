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
        val jacksonVersion = "2.13.+"
        val hopliteVersion = "1.4.9"
        val vertxVersion = "4.1.4"
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk16")
        // Codegen components
        implementation("io.swagger.codegen.v3:swagger-codegen-cli:3.0.26")
        implementation("org.webjars:swagger-ui:3.10.0")

        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
        implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
        implementation("com.google.inject:guice:5.0.1")
        implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
        implementation("com.sksamuel.hoplite:hoplite-yaml:$hopliteVersion")
        implementation("io.github.microutils:kotlin-logging-jvm:2.0.10")
        implementation("io.vertx:vertx-core:$vertxVersion")
        implementation("io.vertx:vertx-web:$vertxVersion")
        implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")

        // Jackson databind required by log4j2 to read yaml configuration files
        runtimeOnly("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
        runtimeOnly("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
        runtimeOnly("org.apache.logging.log4j:log4j-slf4j18-impl:2.14.0")

        testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
        testImplementation("io.vertx:vertx-junit5:$vertxVersion")
        testImplementation("io.vertx:vertx-web-client:$vertxVersion")
        testImplementation("org.mockito:mockito-core:3.+")
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
