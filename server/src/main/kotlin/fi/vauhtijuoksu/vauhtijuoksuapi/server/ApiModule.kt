package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.ApiRouterImpl
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions

class ApiModule : AbstractModule() {
    override fun configure() {
        bind(ApiRouter::class.java).to(ApiRouterImpl::class.java)
    }

    @Provides
    @Singleton
    fun getVertx(): Vertx {
        return Vertx.vertx()
    }

    @Provides
    @Singleton
    fun getHttpServer(vertx: Vertx): HttpServer {
        return vertx.createHttpServer(HttpServerOptions().setPort(8080))
    }
}
