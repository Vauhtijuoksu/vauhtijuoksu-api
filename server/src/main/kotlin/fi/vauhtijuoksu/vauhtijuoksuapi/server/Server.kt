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
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.SubRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.WebsocketRouterForModels
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.ConfigurationModule
import io.vertx.core.Vertx
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
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import jakarta.inject.Inject
import jakarta.inject.Named
import mu.KotlinLogging

private fun HttpServerRequest.referer(): String? = headers().get("referer")
private var Session.referer: String?
    get() = get("referer")
    set(value): Unit {
        put("referer", value)
    }

@Suppress("LongParameterList")
class Server @Inject constructor(
    private val httpServer: HttpServer,
    val router: Router,
    // Needed for guice to find the correct implementation
    private val subRouters: List<@JvmSuppressWildcards SubRouter>,
    private val wsHandler: WebsocketRouterForModels<*>,
    @Named(AUTHENTICATED_CORS) val corsHandler: CorsHandler,
    private val sessionHandler: SessionHandler,
    private val oAuthProvider: OAuth2Auth,
    private val oauth2AuthHandler: OAuth2AuthHandler,
) : CoroutineVerticle() {
    private val logger = KotlinLogging.logger {}

    @Suppress("LongMethod") // TODO separate error handler
    override suspend fun start() {
        router.route().handler(sessionHandler)

        httpServer.webSocketHandler(wsHandler.handler())

        router.get("/login")
            .handler(
                PlatformHandler { // Need a platform handler or some other that is allowed before auth handler
                    if (it.session().referer == null) {
                        it.session().referer = it.request().referer()
                    }
                    it.next()
                },
            )
            .handler(oauth2AuthHandler)
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

        subRouters.forEach {
            it.configure(router)
        }

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
                        logger.warn(cause) { "Unexpected status code ${ctx.statusCode()} in failure handler" }
                        ctx.response().setStatusCode(INTERNAL_SERVER_ERROR).end()
                    }
                }
            }
        }
        httpServer.requestHandler(router)
        httpServer.listen().coAwait()
    }

    override suspend fun stop() {
        httpServer.close().coAwait()
    }
}

fun main() {
    val injector = Guice.createInjector(
        ConfigurationModule(),
        ApiModule(),
        AuthModule(),
        DatabaseModule(),
    )
    val server = injector.getInstance(Server::class.java)
    val vertx = injector.getInstance(Vertx::class.java)
    vertx.deployVerticle(server)
}
