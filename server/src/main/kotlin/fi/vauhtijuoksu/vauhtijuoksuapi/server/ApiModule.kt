package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.TypeLiteral
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants.Companion.AUTHENTICATED_CORS
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants.Companion.PUBLIC_CORS
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PatchInputValidator
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PostInputValidator
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.RedisConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.ServerConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.donation.DonationPatchInputValidator
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.donation.DonationPostInputValidator
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.gamedata.GameDataPatchInputValidator
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.gamedata.GameDataPostInputValidator
import io.vertx.core.Vertx
import io.vertx.core.http.CookieSameSite
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.SessionHandler
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.ext.web.sstore.SessionStore
import io.vertx.ext.web.sstore.redis.RedisSessionStore
import io.vertx.redis.client.Redis
import io.vertx.redis.client.RedisOptions
import jakarta.inject.Named
import java.time.Clock
import java.time.ZoneId

class ApiModule : AbstractModule() {
    override fun configure() {
        bind(object : TypeLiteral<PostInputValidator<GameData>>() {}).to(GameDataPostInputValidator::class.java)
        bind(object : TypeLiteral<PatchInputValidator<GameData>>() {}).to(GameDataPatchInputValidator::class.java)
        bind(object : TypeLiteral<PostInputValidator<Donation>>() {}).to(DonationPostInputValidator::class.java)
        bind(object : TypeLiteral<PatchInputValidator<Donation>>() {}).to(DonationPatchInputValidator::class.java)
    }

    @Provides
    @Singleton
    fun getVertx(): Vertx = Vertx.vertx()

    @Provides
    @Singleton
    fun getRouter(vertx: Vertx): Router = Router.router(vertx)

    @Provides
    @Singleton
    fun getHttpServer(
        vertx: Vertx,
        conf: ServerConfiguration,
    ): HttpServer = vertx.createHttpServer(HttpServerOptions().setPort(conf.port))

    @Provides
    @Singleton
    fun getSessionStore(
        vertx: Vertx,
        redisConf: RedisConfiguration,
    ): SessionStore =
        if (redisConf.enabled) {
            RedisSessionStore.create(
                vertx,
                Redis.createClient(
                    vertx,
                    RedisOptions().setEndpoints(listOf("redis://${redisConf.host}")).setPassword(redisConf.password),
                ),
            )
        } else {
            LocalSessionStore.create(vertx)
        }

    @Provides
    @Singleton
    fun getSessionHandler(
        conf: ServerConfiguration,
        sessionStore: SessionStore,
    ): SessionHandler =
        SessionHandler
            .create(sessionStore)
            .setCookieSameSite(CookieSameSite.NONE)
            .setCookieSecureFlag(conf.sessionCookieSecure)

    @Provides
    @Singleton
    @Named(AUTHENTICATED_CORS)
    fun getCorsHandlerAuth(conf: ServerConfiguration): CorsHandler =
        CorsHandler
            .create()
            .addOriginsWithRegex(conf.corsHeaders)
            .allowCredentials(true)
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.POST)
            .allowedMethod(HttpMethod.OPTIONS)
            .allowedMethod(HttpMethod.PATCH)
            .allowedMethod(HttpMethod.DELETE)

    @Provides
    @Singleton
    @Named(PUBLIC_CORS)
    fun getCorsHandlerPublic(): CorsHandler =
        CorsHandler
            .create()
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.POST)
            .allowedMethod(HttpMethod.OPTIONS)
            .allowedMethod(HttpMethod.PATCH)
            .allowedMethod(HttpMethod.DELETE)

    @Provides
    @Singleton
    fun clock(): Clock = Clock.system(ZoneId.of("Europe/Helsinki"))
}
