package fi.vauhtijuoksu.vauhtijuoksuapi.server

import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import java.util.concurrent.CountDownLatch

class Server {
    private val vertx = Vertx.vertx()
    private val server = vertx.createHttpServer(HttpServerOptions().setPort(8080))
    private val router = Router.router(vertx)

    init {
        router.get().handler { ctx -> ctx.response().end("Hello world") }
        server.requestHandler(router)
    }

    fun start() {
        server.listen()
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
