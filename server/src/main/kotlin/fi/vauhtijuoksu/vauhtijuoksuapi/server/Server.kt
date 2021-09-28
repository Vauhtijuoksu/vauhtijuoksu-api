package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import java.time.Instant
import java.util.*
import java.util.concurrent.CountDownLatch

class Server {
    private val vertx = Vertx.vertx()

    @Suppress("MagicNumber") // Configuration is not yet implemented
    private val httpServer = vertx.createHttpServer(HttpServerOptions().setPort(8080))
    private val router = Router.router(vertx)

    init {
        router.get("/gamedata").handler { ctx ->
            val gameData = GameData("Tetris", "jsloth", Date.from(Instant.now()), Date.from(Instant.now()), "any%", "PC", "1970", null, "tetris.png", "jiisloth")
            ctx.response().end(jacksonObjectMapper().writeValueAsString(gameData))
        }
        router.get().handler { ctx -> ctx.response().end("Hello world") }
        httpServer.requestHandler(router)
    }

    fun start() {
        httpServer.listen()
    }

    fun stop() {
        val latch = CountDownLatch(1)
        vertx.close { latch.countDown() }
        latch.await()
    }
}

fun main() {
    Server().start()
}
