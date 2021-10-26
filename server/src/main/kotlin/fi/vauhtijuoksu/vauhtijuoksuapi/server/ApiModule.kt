package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.TypeLiteral
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PatchInputValidator
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PostInputValidator
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.ServerConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.DonationPatchInputValidator
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.DonationPostInputValidator
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.GameDataPostInputValidator
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.auth.authentication.AuthenticationProvider
import io.vertx.ext.auth.htpasswd.HtpasswdAuth
import io.vertx.ext.auth.htpasswd.HtpasswdAuthOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.BasicAuthHandler
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.SessionHandler
import io.vertx.ext.web.sstore.LocalSessionStore

class ApiModule : AbstractModule() {

    override fun configure() {
        bind(object : TypeLiteral<PostInputValidator<GameData>>() {}).to(GameDataPostInputValidator::class.java)
        bind(object : TypeLiteral<PostInputValidator<Donation>>() {}).to(DonationPostInputValidator::class.java)
        bind(object : TypeLiteral<PatchInputValidator<Donation>>() {}).to(DonationPatchInputValidator::class.java)
    }

    @Provides
    @Singleton
    fun getVertx(): Vertx {
        return Vertx.vertx()
    }

    @Provides
    @Singleton
    fun getRouter(vertx: Vertx): Router {
        return Router.router(vertx)
    }

    @Provides
    @Singleton
    fun getHttpServer(vertx: Vertx, conf: ServerConfiguration): HttpServer {
        return vertx.createHttpServer(HttpServerOptions().setPort(conf.port))
    }

    @Provides
    @Singleton
    fun getSessionHandler(vertx: Vertx): SessionHandler {
        return SessionHandler.create(LocalSessionStore.create(vertx))
    }

    @Provides
    @Singleton
    fun getAuthenticationProvider(vertx: Vertx, conf: ServerConfiguration): AuthenticationProvider {
        return HtpasswdAuth.create(vertx, HtpasswdAuthOptions().setHtpasswdFile(conf.htpasswdFileLocation))
    }

    @Provides
    @Singleton
    fun getAuthenticationHandler(authenticationProvider: AuthenticationProvider): AuthenticationHandler {
        return BasicAuthHandler.create(authenticationProvider)
    }

    @Provides
    @Singleton
    fun getCorsHandler(conf: ServerConfiguration): CorsHandler {
        return CorsHandler.create(conf.corsHeader)
    }
}
