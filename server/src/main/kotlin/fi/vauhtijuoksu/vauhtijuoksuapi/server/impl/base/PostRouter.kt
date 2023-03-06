package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.UserError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model
import fi.vauhtijuoksu.vauhtijuoksuapi.server.ApiConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PartialRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PostInputValidator
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import mu.KotlinLogging

open class PostRouter<M : Model>(
    private val authenticationHandler: AuthenticationHandler,
    private val authenticatedEndpointCorsHandler: CorsHandler,
    private val db: VauhtijuoksuDatabase<M>,
    private val toModel: (JsonObject) -> M,
    private val toJson: (M) -> JsonObject,
    private val postInputValidator: PostInputValidator<M>,
) : PartialRouter {
    private val logger = KotlinLogging.logger {}

    override fun configure(router: Router, basepath: String) {
        router.post(basepath)
            .handler(authenticatedEndpointCorsHandler)
            .handler(BodyHandler.create())
            .handler(authenticationHandler)
            .handler { ctx ->
                val record: M
                try {
                    val jsonBody = ctx.body().asJsonObject() ?: throw UserError("Body is required on POST")
                    record = toModel(jsonBody)
                    logger.debug { "Inserting a new record object $record" }
                } catch (e: IllegalArgumentException) {
                    throw UserError("Error parsing record object from ${ctx.body().asString()} because ${e.message}", e)
                }

                val validationMessage = postInputValidator.validate(record)
                if (validationMessage != null) {
                    throw UserError(validationMessage)
                }

                db.add(record)
                    .onFailure(ctx::fail)
                    .onSuccess {
                        logger.info { "Inserted record $record" }
                        ctx.response().setStatusCode(ApiConstants.CREATED)
                            .end(toJson(record).encode())
                    }
            }
    }
}
