plugins {
    id("vauhtijuoksu-api.kotlin-conventions")
}
dependencies {
    api(project(path = ":models"))
    api(libs.vertx.core)
    api(libs.arrow.kt)
    implementation(libs.kotlinx.coroutines)
}
