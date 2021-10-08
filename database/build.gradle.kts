plugins {
    id("vauhtijuoksu-api.kotlin-common-conventions")
}

dependencies {
    implementation(project(path = ":models"))
    implementation("com.google.inject:guice")
    implementation("io.github.microutils:kotlin-logging-jvm")
    implementation("io.vertx:vertx-core")
    implementation("io.vertx:vertx-lang-kotlin")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    runtimeOnly("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    runtimeOnly("com.fasterxml.jackson.core:jackson-databind")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j18-impl")

    testImplementation("io.vertx:vertx-junit5")
}
