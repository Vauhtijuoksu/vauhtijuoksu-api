package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.MissingEntityException
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.UserError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PartialRouter
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.CorsHandler
import java.util.UUID

open class GetRouter<M : Model>(
    private val publicEndpointCorsHandler: CorsHandler,
    private val db: VauhtijuoksuDatabase<M>,
) : PartialRouter {
    override fun configure(router: Router, basepath: String) {
        configureGetAll(router, basepath)
        configureGetSingle(router, basepath)
    }

    private fun configureGetAll(router: Router, basepath: String) {
        router.get(basepath)
            .handler(publicEndpointCorsHandler)
            .handler { ctx ->
                db.getAll()
                    .onFailure { t ->
                        ctx.fail(ServerError("Failed to retrieve record because of ${t.message}"))
                    }
                    .onSuccess { all -> ctx.response().end(jacksonObjectMapper().writeValueAsString(all)) }
            }
    }

    private fun configureGetSingle(router: Router, basepath: String) {
        router.get("$basepath/:id")
            .handler(publicEndpointCorsHandler)
            .handler { ctx ->
                val id: UUID
                try {
                    id = UUID.fromString(ctx.pathParam("id"))
                } catch (_: IllegalArgumentException) {
                    throw UserError("Not UUID: ${ctx.pathParam("id")}")
                }
                db.getById(id)
                    .onFailure(ctx::fail)
                    .onSuccess { gameData ->
                        if (gameData == null) {
                            ctx.fail(MissingEntityException("No entity with id $id"))
                        } else {
                            ctx.response().end(jacksonObjectMapper().writeValueAsString(gameData))
                        }
                    }
            }
    }
}
