package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.inject.Guice
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiRouter
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import java.time.Instant
import java.util.*
import java.util.concurrent.CountDownLatch
import javax.inject.Inject

class Server @Inject constructor(private val vertx: Vertx, private val httpServer: HttpServer, apiRouter: ApiRouter){

    @Suppress("MagicNumber") // Configuration is not yet implemented
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
    val injector = Guice.createInjector(ApiModule());
    val server = injector.getInstance(Server::class.java)
    server.start()
}
