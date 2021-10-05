package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiRouter
import io.vertx.ext.web.Router
import java.util.*
import javax.inject.Inject

class ApiRouterImpl @Inject constructor(private val router: Router, private val db: VauhtijuoksuDatabase) : ApiRouter {
    init {
        router.route().handler { ctx ->
            ctx.response().putHeader("content-type", "application/json")
            ctx.next()
        }
        router.get("/gamedata").handler { ctx ->
            db.getAll()
                .onFailure { ctx.response().setStatusCode(500).end() }
                .onSuccess { all -> ctx.response().end(jacksonObjectMapper().writeValueAsString(all)) }
        }
        router.get("/gamedata/:id").handler { ctx ->
            val id: UUID
            try {
                id = UUID.fromString(ctx.pathParam("id"))
            } catch (e: IllegalArgumentException) {
                ctx.response().setStatusCode(500).end()
                return@handler
            }
            db.getById(id)
                .onFailure { ctx.response().setStatusCode(500).end() }
                .onSuccess { maybeGameData ->
                    if (maybeGameData.isEmpty) {
                        ctx.response().setStatusCode(404).end();
                    } else {
                        ctx.response().end(jacksonObjectMapper().writeValueAsString(maybeGameData.get()))
                    }
                }
        }
    }

    override fun router(): Router {
        return router
    }
}
