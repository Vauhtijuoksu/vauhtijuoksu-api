plugins {
    id("vauhtijuoksu-api.kotlin-conventions")
}
dependencies {
    api(project(path = ":models"))
    api("io.vertx:vertx-core")
}
