package fi.vauhtijuoksu.vauhtijuoksuapi.server

import io.vertx.core.Vertx

fun main() {
    val vertx = Vertx.vertx()
    val client = vertx.createWebSocketClient()

    client.webSocket().connect(80, "api.localhost", "/gamedata")
        .onSuccess {
            it.textMessageHandler(::println)
        }.onFailure {
            println("Failed to connect: ${it.message}")
            vertx.close()
        }
}
