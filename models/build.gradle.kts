plugins {
    id("vauhtijuoksu-api.implementation-conventions")
}

dependencies {
    implementation(libs.jackson.annotations)

    testImplementation(libs.vertx.core)
    testImplementation(libs.jackson.module.kotlin)
}
