package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import fi.vauhtijuoksu.vauhtijuoksuapi.server.ApiConstants.Companion.METHOD_NOT_ALLOWED
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PartialRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.SubRouter
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.web.Router

open class BaseRouter
protected constructor(
    private val endpoint: String,
    private val routers: List<PartialRouter>,
) : SubRouter {
    init {
        DatabindCodec.mapper()
            .registerModule(kotlinModule())
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        if (!endpoint.startsWith("/")) {
            throw IllegalArgumentException("Endpoint should start with /")
        }
    }

    override fun configure(router: Router) {
        configureContentType(router)
        configureSubRouters(router)
        configureNotAllowedMethods(router)
    }

    private fun configureContentType(router: Router) {
        router.route("$endpoint*").handler { ctx ->
            ctx.response().putHeader("content-type", "application/json")
            ctx.next()
        }
    }

    private fun configureSubRouters(router: Router) {
        for (subRouter in routers) {
            subRouter.configure(router, endpoint)
        }
    }

    private fun configureNotAllowedMethods(router: Router) {
        router.route(endpoint).handler { ctx ->
            ctx.response().setStatusCode(METHOD_NOT_ALLOWED).end()
        }

        router.route("$endpoint/:id").handler { ctx ->
            ctx.response().setStatusCode(METHOD_NOT_ALLOWED).end()
        }
    }
}
