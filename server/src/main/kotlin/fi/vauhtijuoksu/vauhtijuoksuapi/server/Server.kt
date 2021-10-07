package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.google.inject.Guice
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import java.util.concurrent.CountDownLatch
import javax.inject.Inject

class Server @Inject constructor(private val httpServer: HttpServer) {
    private val vertx = Vertx.vertx()
    private val router = Router.router(vertx)

    init {
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
    val injector = Guice.createInjector(ConfigurationModule(), ApiModule())
    val server = injector.getInstance(Server::class.java)
    server.start()
}
