package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.UserError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model
import fi.vauhtijuoksu.vauhtijuoksuapi.server.ApiConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiModel
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PartialRouter
import io.vertx.core.Future
import io.vertx.core.json.jackson.DatabindCodec.mapper
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.AuthorizationHandler
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import mu.KotlinLogging
import java.util.UUID

open class PatchRouter<M : Model, ApiRepresentation : ApiModel<M>>(
    private val authenticationHandler: AuthenticationHandler,
    private val adminRequired: AuthorizationHandler,
    private val authenticatedEndpointCorsHandler: CorsHandler,
    private val db: VauhtijuoksuDatabase<M>,
    private val patchValidator: (M) -> String?,
    private val toApiRepresentation: (M) -> ApiRepresentation,
) : PartialRouter {
    private val logger = KotlinLogging.logger {}

    override fun configure(
        router: Router,
        basepath: String,
    ) {
        router
            .patch("$basepath/:id")
            .handler(authenticatedEndpointCorsHandler)
            .handler(BodyHandler.create())
            .handler(authenticationHandler)
            .handler(adminRequired)
            .handler { ctx ->
                val id: UUID
                try {
                    id = UUID.fromString(ctx.pathParam("id"))
                } catch (_: IllegalArgumentException) {
                    throw UserError("Not UUID: ${ctx.pathParam("id")}")
                }

                val jsonBody = ctx.body().asJsonObject() ?: throw UserError("Body is required on PATCH")
                logger.debug { "Patching a record with object $jsonBody" }

                db
                    .getById(id)
                    .map { merge(ctx, it) }
                    .compose(db::update)
                    .compose { respond(id, ctx) }
                    .onFailure(ctx::fail)
            }
    }

    private fun merge(
        ctx: RoutingContext,
        existingData: M,
    ): M {
        val oldData = mapper().readerForUpdating(toApiRepresentation(existingData))
        val mergedData: M =
            try {
                oldData.readValue<ApiRepresentation>(ctx.body().asString()).toModel()
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Throwable,
            ) {
                throw UserError("Error patching object: ${e.message}", e)
            }

        val validationMessage = patchValidator(mergedData)
        if (validationMessage != null) {
            throw UserError("Invalid input: $validationMessage")
        }
        return mergedData
    }

    protected open fun respond(
        updatedId: UUID,
        ctx: RoutingContext,
    ): Future<Void> =
        db
            .getById(updatedId)
            .map { updatedRecord ->
                logger.info { "Patched record $updatedRecord" }
                ctx
                    .response()
                    .setStatusCode(ApiConstants.OK)
                    .end(toApiRepresentation(updatedRecord).toJson().encode())
            }.mapEmpty()
}
