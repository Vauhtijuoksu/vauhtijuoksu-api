package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiRouter
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import java.util.UUID
import javax.inject.Inject

class ApiRouterImpl @Inject constructor(private val router: Router, private val db: VauhtijuoksuDatabase) : ApiRouter {
    companion object {
        const val NOT_FOUND = 404
        const val INTERNAL_SERVER_ERROR = 500
        const val NOT_IMPLEMENTED = 501
    }

    private fun notImplemented(ctx: RoutingContext) {
        @Suppress("MagicNumber") // Context makes it clear
        ctx.response().setStatusCode(NOT_IMPLEMENTED).end()
    }

    init {
        router.route().handler { ctx ->
            ctx.response().putHeader("content-type", "application/json")
            ctx.next()
        }
        router.get("/gamedata").handler { ctx ->
            db.getAll()
                .onFailure { ctx.response().setStatusCode(INTERNAL_SERVER_ERROR).end() }
                .onSuccess { all -> ctx.response().end(jacksonObjectMapper().writeValueAsString(all)) }
        }
        router.post("/gamedata").handler(this::notImplemented)

        router.get("/gamedata/:id").handler { ctx ->
            val id: UUID
            try {
                id = UUID.fromString(ctx.pathParam("id"))
            } catch (e: IllegalArgumentException) {
                ctx.response().setStatusCode(INTERNAL_SERVER_ERROR).end(e.message)
                return@handler
            }
            db.getById(id)
                .onFailure { ctx.response().setStatusCode(INTERNAL_SERVER_ERROR).end() }
                .onSuccess { maybeGameData ->
                    if (maybeGameData.isEmpty) {
                        ctx.response().setStatusCode(NOT_FOUND).end()
                    } else {
                        ctx.response().end(jacksonObjectMapper().writeValueAsString(maybeGameData.get()))
                    }
                }
        }
        router.patch("/gamedata/:id").handler(this::notImplemented)
    }

    override fun router(): Router {
        return router
    }
}
