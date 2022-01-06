plugins {
    id("vauhtijuoksu-api.implementation-conventions")
}

dependencies {
    implementation(project(path = ":models"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.google.inject:guice")
    implementation("io.github.microutils:kotlin-logging-jvm")
    implementation("io.vertx:vertx-core")
    implementation("io.vertx:vertx-lang-kotlin")
    implementation("io.vertx:vertx-pg-client")
    implementation("io.vertx:vertx-sql-client-templates")
    implementation("org.flywaydb:flyway-core")
    implementation("org.postgresql:postgresql")

    runtimeOnly("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    runtimeOnly("com.fasterxml.jackson.core:jackson-databind")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j18-impl")

    testImplementation(project(path = ":test-data"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("io.vertx:vertx-junit5")
}
