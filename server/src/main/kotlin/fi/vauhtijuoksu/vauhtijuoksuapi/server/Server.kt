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
import fi.vauhtijuoksu.vauhtijuoksuapi.server.ApiConstants.Companion.USER_ERROR_CODES
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants.Companion.AUTHENTICATED_CORS
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.ConfigurationModule
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.PlayerInfoRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.StreamMetadataRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.donation.DonationsRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.gamedata.GameDataRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentivecodes.IncentiveCodeRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives.IncentivesRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.participants.ParticipantsRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.timers.TimerRouter
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.auth.oauth2.OAuth2Auth
import io.vertx.ext.web.Router
import io.vertx.ext.web.Session
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.HttpException
import io.vertx.ext.web.handler.OAuth2AuthHandler
import io.vertx.ext.web.handler.PlatformHandler
import io.vertx.ext.web.handler.SessionHandler
import jakarta.inject.Inject
import jakarta.inject.Named
import mu.KotlinLogging
import java.util.concurrent.CountDownLatch
import kotlin.system.exitProcess

private fun HttpServerRequest.referer(): String? = headers().get("referer")

private var Session.referer: String?
    get() = get("referer")
    set(value): Unit {
        put("referer", value)
    }

@Suppress("LongParameterList")
class Server
    @Inject
    constructor(
        private val httpServer: HttpServer,
        router: Router,
        gameDataRouter: GameDataRouter,
        donationsRouter: DonationsRouter,
        streamMetadataRouter: StreamMetadataRouter,
        playerInfoRouter: PlayerInfoRouter,
        incentivesRouter: IncentivesRouter,
        incentiveCodeRouter: IncentiveCodeRouter,
        participantsRouter: ParticipantsRouter,
        timerRouter: TimerRouter,
        @Named(AUTHENTICATED_CORS) corsHandler: CorsHandler,
        sessionHandler: SessionHandler,
        oAuthProvider: OAuth2Auth,
        oauth2AuthHandler: OAuth2AuthHandler,
    ) {
        private val logger = KotlinLogging.logger {}

        init {
            router.route().handler(sessionHandler)

            router
                .get("/login")
                .handler(
                    PlatformHandler {
                        // Need a platform handler or some other that is allowed before auth handler
                        if (it.session().referer == null) {
                            it.session().referer = it.request().referer()
                        }
                        it.next()
                    },
                ).handler(oauth2AuthHandler)
                .handler {
                    if (it.session().referer != null) {
                        it.redirect(it.session().referer)
                    } else {
                        it.response().end()
                    }
                }

            router.post("/logout").handler {
                it.user()?.run { oAuthProvider.revoke(this) }
                it.session().destroy()
                it.request().end()
            }

            oauth2AuthHandler.setupCallback(router.route("/callback"))

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
            participantsRouter.configure(router)
            timerRouter.configure(router)
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
                        if (ctx.statusCode() in USER_ERROR_CODES) {
                            ctx.response().setStatusCode(ctx.statusCode()).end()
                        } else {
                            logger.warn { "Unexpected status code ${ctx.statusCode()} in failure handler with error $cause" }
                            ctx.response().setStatusCode(INTERNAL_SERVER_ERROR).end()
                        }
                    }
                }
            }
            httpServer.requestHandler(router)
        }

        fun start() {
            val latch = CountDownLatch(1)
            httpServer
                .listen()
                .onSuccess {
                    logger.info { "Server listening on port ${httpServer.actualPort()}" }
                    latch.countDown()
                }.onFailure { t ->
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
    val injector = Guice.createInjector(ConfigurationModule(), ApiModule(), AuthModule(), DatabaseModule())
    val server = injector.getInstance(Server::class.java)
    server.start()
}
