package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiRouter
import io.vertx.ext.web.Router
import javax.inject.Inject

class ApiRouterImpl @Inject constructor(private val router: Router, private val db: VauhtijuoksuDatabase) : ApiRouter {
    init {
        router.get("/gamedata").handler { ctx ->
            db.getAll()
                .onFailure { ctx.response().setStatusCode(500).end() }
                .onSuccess { all -> ctx.response().end(jacksonObjectMapper().writeValueAsString(all)) }
        }
    }

    override fun router(): Router {
        return router
    }
}
