plugins {
    id("vauhtijuoksu-api.kotlin-conventions")
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-annotations")

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("io.vertx:vertx-core")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
