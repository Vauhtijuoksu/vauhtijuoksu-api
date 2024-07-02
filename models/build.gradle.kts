plugins {
    id("vauhtijuoksu-api.implementation-conventions")
}

dependencies {
    api(libs.jackson.annotations)
    api(libs.kotlinx.coroutines)

    testImplementation(libs.vertx.core)
    testImplementation(libs.jackson.module.kotlin)
}
