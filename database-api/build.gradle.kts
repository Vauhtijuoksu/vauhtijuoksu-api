plugins {
    id("vauhtijuoksu-api.kotlin-conventions")
}
dependencies {
    api(project(path = ":models"))
    api(libs.vertx.core)
}
