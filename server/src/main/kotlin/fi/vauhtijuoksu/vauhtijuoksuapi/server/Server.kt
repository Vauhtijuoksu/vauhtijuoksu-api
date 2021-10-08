package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.google.inject.Guice
import fi.vauhtijuoksu.vauhtijuoksuapi.database.DatabaseModule
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiRouter
import io.vertx.core.http.HttpServer
import mu.KotlinLogging
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import kotlin.system.exitProcess

class Server @Inject constructor(private val httpServer: HttpServer, apiRouter: ApiRouter) {
    private val logger = KotlinLogging.logger {}

    init {
        httpServer.requestHandler(apiRouter.router())
    }

    fun start() {
        val latch = CountDownLatch(1)
        httpServer.listen()
            .onSuccess {
                logger.info { "Server listening on port ${httpServer.actualPort()}" }
                latch.countDown()
            }
            .onFailure { t ->
                logger.error { "Server could not start because ${t.message}. Exiting" }
                exitProcess(1)
            }
    }

    fun stop() {
        val latch = CountDownLatch(1)
        httpServer.close { latch.countDown() }
        latch.await()
    }
}

fun main() {
    val injector = Guice.createInjector(ConfigurationModule(), ApiModule(), DatabaseModule())
    val server = injector.getInstance(Server::class.java)
    server.start()
}
