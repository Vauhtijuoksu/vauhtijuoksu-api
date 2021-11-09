package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.google.inject.Guice
import fi.vauhtijuoksu.vauhtijuoksuapi.database.DatabaseModule
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants.Companion.PUBLIC_CORS
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.DonationsRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.GameDataRouter
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.SessionHandler
import mu.KotlinLogging
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import javax.inject.Named
import kotlin.system.exitProcess

class Server @Inject constructor(
    private val httpServer: HttpServer,
    router: Router,
    gameDataRouter: GameDataRouter,
    donationsRouter: DonationsRouter,
    sessionHandler: SessionHandler,
    @Named(PUBLIC_CORS) corsHandler: CorsHandler,
) {
    private val logger = KotlinLogging.logger {}

    init {
        router.options().handler(corsHandler)
        router.options().handler { ctx ->
            ctx.response().headers().add(
                HttpHeaders.ALLOW,
                "${HttpMethod.GET}, ${HttpMethod.POST}, ${HttpMethod.PATCH}, ${HttpMethod.OPTIONS}, ${HttpMethod.DELETE}"
            )
            ctx.response().end()
        }
        gameDataRouter.configure(router)
        donationsRouter.configure(router)
        httpServer.requestHandler(router)
        router.route().handler(sessionHandler)
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
