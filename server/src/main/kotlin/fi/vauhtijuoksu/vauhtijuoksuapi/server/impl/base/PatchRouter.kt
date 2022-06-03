package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.MissingEntityException
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.UserError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model
import fi.vauhtijuoksu.vauhtijuoksuapi.server.ApiConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiModel
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PartialRouter
import io.vertx.core.json.jackson.DatabindCodec.mapper
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import mu.KotlinLogging
import java.util.UUID

open class PatchRouter<M : Model, ApiRepresentation : ApiModel<M>>(
    private val authenticationHandler: AuthenticationHandler,
    private val authenticatedEndpointCorsHandler: CorsHandler,
    private val db: VauhtijuoksuDatabase<M>,
    private val patchValidator: (M) -> String?,
    private val toApiRepresentation: (M) -> ApiRepresentation,
) : PartialRouter {
    private val logger = KotlinLogging.logger {}

    override fun configure(router: Router, basepath: String) {
        router.patch("$basepath/:id")
            .handler(authenticatedEndpointCorsHandler)
            .handler(BodyHandler.create())
            .handler(authenticationHandler)
            .handler { ctx ->
                val id: UUID
                try {
                    id = UUID.fromString(ctx.pathParam("id"))
                } catch (_: IllegalArgumentException) {
                    throw UserError("Not UUID: ${ctx.pathParam("id")}")
                }

                val jsonBody = ctx.body().asJsonObject() ?: throw UserError("Body is required on PATCH")
                logger.debug ( "Patching a record with object $jsonBody" )

                db.getById(id)
                    .map { res ->
                        if (res == null) {
                            throw MissingEntityException("Could not find record with id $id for update")
                        }

                        val oldData = mapper().readerForUpdating(toApiRepresentation(res))
                        val mergedData: M = try {
                            oldData.readValue<ApiRepresentation>(ctx.body().asString()).toModel()
                        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                            throw UserError("Error patching object: ${e.message}", e)
                        }

                        val validationMessage = patchValidator(mergedData)
                        if (validationMessage != null) {
                            throw UserError("Invalid input: $validationMessage")
                        }
                        return@map mergedData
                    }.compose { db.update(it) }
                    .map {
                        // Record was removed after fetching it, before patching
                        return@map it ?: throw MissingEntityException("Could not find record with id $id for update")
                    }.onSuccess { updatedRecord ->
                        logger.info { "Patched record $updatedRecord" }
                        ctx.response().setStatusCode(ApiConstants.OK)
                            .end(toApiRepresentation(updatedRecord).toJson().encode())
                    }
                    .onFailure(ctx::fail)
            }
    }
}
