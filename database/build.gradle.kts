plugins {
    id("vauhtijuoksu-api.kotlin-common-conventions")
}

dependencies {
    implementation(project(path = ":models"))
    implementation("com.google.inject:guice")
    implementation("io.vertx:vertx-core")
    implementation("io.vertx:vertx-lang-kotlin")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("io.vertx:vertx-junit5")
}
