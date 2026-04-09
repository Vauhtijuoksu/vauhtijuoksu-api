plugins {
    id("vauhtijuoksu-api.kotlin-conventions")
}

dependencies {
    implementation(project(path = ":models"))
    implementation(libs.instancio.kotlin)
}
