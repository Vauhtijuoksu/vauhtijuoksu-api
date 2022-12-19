/**
 * Configuration for all projects containing Kotlin code, including interfaces and test utils
 */
plugins {
    id("vauhtijuoksu-api.common-conventions")
    id("io.gitlab.arturbosch.detekt")
}

dependencies {
    constraints {
        val jacksonVersion = "2.14.1"
        val hopliteVersion = "2.1.5"
        val vertxVersion = "4.3.6"
        val testContainersVersion = "1.17.6"
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk17")
        // Codegen components
        implementation("io.swagger.codegen.v3:swagger-codegen-cli:3.0.36")
        implementation("org.webjars:swagger-ui:4.15.5")

        implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
        implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
        implementation("com.google.inject:guice:5.1.0")
        implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
        implementation("com.sksamuel.hoplite:hoplite-yaml:$hopliteVersion")
        implementation("io.github.microutils:kotlin-logging-jvm:2.1.23")
        implementation("io.vertx:vertx-auth-htpasswd:$vertxVersion")
        implementation("io.vertx:vertx-core:$vertxVersion")
        implementation("io.vertx:vertx-web:$vertxVersion")
        implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
        implementation("io.vertx:vertx-pg-client:$vertxVersion")
        implementation("io.vertx:vertx-sql-client-templates:$vertxVersion")
        implementation("org.flywaydb:flyway-core:9.10.1")
        implementation("org.postgresql:postgresql:42.5.1")

        // Jackson databind required by log4j2 to read yaml configuration files
        runtimeOnly("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
        runtimeOnly("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
        runtimeOnly("org.apache.logging.log4j:log4j-slf4j18-impl:2.18.0")

        testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
        testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")
        testImplementation("org.testcontainers:postgresql:$testContainersVersion")
        testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
        testImplementation("io.vertx:vertx-junit5:$vertxVersion")
        testImplementation("io.vertx:vertx-web-client:$vertxVersion")
        testImplementation("org.mockito:mockito-core:3.+")
        testImplementation("org.mockito:mockito-junit-jupiter:3.+")
    }
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.20.0")

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
            jvmTarget = "17"
        }
    }

    // Remove timestamps and always use same file order in jars to make builds reproducible
    withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}
