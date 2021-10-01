package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.google.inject.Guice
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiRouter
import io.vertx.core.http.HttpServer
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import kotlin.system.exitProcess

class Server @Inject constructor(private val httpServer: HttpServer, apiRouter: ApiRouter) {
    init {
        httpServer.requestHandler(apiRouter.router())
    }

    fun start() {
        val latch = CountDownLatch(1)
        httpServer.listen { res ->
            if (res.succeeded()) {
                latch.countDown()
            } else {
                // TODO clean up
                println(res.cause())
                exitProcess(1)
            }
        }
    }

    fun stop() {
        val latch = CountDownLatch(1)
        httpServer.close { latch.countDown() }
        latch.await()
    }
}

fun main() {
    val injector = Guice.createInjector(ApiModule())
    val server = injector.getInstance(Server::class.java)
    server.start()
}
