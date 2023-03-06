package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.google.inject.Guice
import fi.vauhtijuoksu.vauhtijuoksuapi.database.DatabaseModule
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.MissingEntityException
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.UserError
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.VauhtijuoksuException
import fi.vauhtijuoksu.vauhtijuoksuapi.server.ApiConstants.Companion.BAD_REQUEST
import fi.vauhtijuoksu.vauhtijuoksuapi.server.ApiConstants.Companion.INTERNAL_SERVER_ERROR
import fi.vauhtijuoksu.vauhtijuoksuapi.server.ApiConstants.Companion.NOT_FOUND
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants.Companion.PUBLIC_CORS
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.PlayerInfoRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.StreamMetadataRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.donation.DonationsRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.gamedata.GameDataRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentivecodes.IncentiveCodeRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives.IncentivesRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.players.PlayersRouter
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.HttpException
import mu.KotlinLogging
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import javax.inject.Named
import kotlin.system.exitProcess

@Suppress("LongParameterList")
class Server @Inject constructor(
    private val httpServer: HttpServer,
    router: Router,
    gameDataRouter: GameDataRouter,
    donationsRouter: DonationsRouter,
    streamMetadataRouter: StreamMetadataRouter,
    playerInfoRouter: PlayerInfoRouter,
    incentivesRouter: IncentivesRouter,
    incentiveCodeRouter: IncentiveCodeRouter,
    playersRouter: PlayersRouter,
    @Named(PUBLIC_CORS) corsHandler: CorsHandler,
) {
    private val logger = KotlinLogging.logger {}

    init {
        router.options().handler(corsHandler)
        router.options().handler { ctx ->
            ctx.response().headers().add(
                HttpHeaders.ALLOW,
                "${HttpMethod.GET}, ${HttpMethod.POST}, ${HttpMethod.PATCH}, ${HttpMethod.OPTIONS}, ${HttpMethod.DELETE}",
            )
            ctx.response().end()
        }
        gameDataRouter.configure(router)
        donationsRouter.configure(router)
        streamMetadataRouter.configure(router)
        playerInfoRouter.configure(router)
        incentivesRouter.configure(router)
        incentiveCodeRouter.configure(router)
        playersRouter.configure(router)
        router.route().failureHandler { ctx ->
            when (val cause = ctx.failure()) {
                is UserError -> {
                    logger.warn { "Generic error: ${cause.message}" }
                    ctx.response().setStatusCode(BAD_REQUEST).end(cause.message)
                }
                is MissingEntityException -> {
                    logger.info { "Missing entity: ${cause.message}" }
                    ctx.response().setStatusCode(NOT_FOUND).end(cause.message)
                }
                is ServerError, is VauhtijuoksuException -> {
                    logger.warn { "ServerError: ${cause.message}" }
                    ctx.response().setStatusCode(INTERNAL_SERVER_ERROR).end(cause.message)
                }
                is HttpException -> {
                    ctx.response().setStatusCode(cause.statusCode).end()
                }
                else -> {
                    logger.warn { "Uncaught error: ${cause.message}" }
                    ctx.response().setStatusCode(INTERNAL_SERVER_ERROR).end()
                }
            }
        }

        httpServer.requestHandler(router)
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
