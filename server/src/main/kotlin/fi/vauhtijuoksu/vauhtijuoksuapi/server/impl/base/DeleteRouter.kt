package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.UserError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model
import fi.vauhtijuoksu.vauhtijuoksuapi.server.ApiConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PartialRouter
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.AuthorizationHandler
import io.vertx.ext.web.handler.CorsHandler
import java.util.UUID

open class DeleteRouter<M : Model>(
    private val authenticationHandler: AuthenticationHandler,
    private val adminRequired: AuthorizationHandler,
    private val authenticatedEndpointCorsHandler: CorsHandler,
    private val db: VauhtijuoksuDatabase<M>,
) : PartialRouter {
    override fun configure(
        router: Router,
        basepath: String,
    ) {
        router
            .delete("$basepath/:id")
            .handler(authenticatedEndpointCorsHandler)
            .handler(authenticationHandler)
            .handler(adminRequired)
            .handler { ctx ->
                val id: UUID
                try {
                    id = UUID.fromString(ctx.pathParam("id"))
                } catch (_: IllegalArgumentException) {
                    throw UserError("Not UUID: ${ctx.pathParam("id")}")
                }
                db
                    .delete(id)
                    .onFailure(ctx::fail)
                    .onSuccess { ctx.response().setStatusCode(ApiConstants.NO_CONTENT).end() }
            }
    }
}
