plugins {
    id("vauhtijuoksu-api.implementation-conventions")
}

dependencies {
    implementation(project(path = ":models"))
    implementation(project(path = ":database-api"))
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.guice)
    implementation(libs.mu.logging)
    implementation(libs.vertx.core)
    implementation(libs.vertx.lang.kotlin)
    implementation(libs.vertx.pg.client)
    implementation(libs.vertx.sql.client.templates)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.postgresql)

    runtimeOnly(libs.jackson.dataformat.yaml)
    runtimeOnly(libs.log4j.slf4j18.impl)

    testImplementation(project(path = ":test-data"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.vertx.junit5)
    testImplementation(libs.vertx.lang.kotlincoroutines)
}
