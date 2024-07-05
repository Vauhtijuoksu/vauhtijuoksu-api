package fi.vauhtijuoksu.vauhtijuoksuapi.server

import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun vertxTest() {
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

fun main() {
    val vertx = Vertx.vertx()
    val failing = CoroutineScope(vertx.dispatcher()).launch {
        delay(2000)
        throw Exception("This should not be printed")
    }

    val printing = CoroutineScope(vertx.dispatcher()).launch {
        while (true) {
            println("Still alive")
            println(failing.isActive)
            println(failing.isCompleted)
            println(failing.isCancelled)
            delay(1000)
        }
    }

    failing.invokeOnCompletion {
        printing.cancel()
        vertx.close()
    }
}
