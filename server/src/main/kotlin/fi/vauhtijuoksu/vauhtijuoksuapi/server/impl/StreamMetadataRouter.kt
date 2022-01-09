package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.SingletonDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.UserError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.StreamMetadata
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.model.StreamMetadatApiModel
import io.vertx.core.Future.future
import io.vertx.core.Promise
import io.vertx.core.json.DecodeException
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import javax.inject.Inject
import javax.inject.Named

// Throwing as soon as some validation fails is the simplest way
@Suppress("ThrowsCount")
class StreamMetadataRouter
@Inject constructor(
    private val db: SingletonDatabase<StreamMetadata>,
    private val authenticationHandler: AuthenticationHandler,
    @Named(DependencyInjectionConstants.AUTHENTICATED_CORS)
    private val authenticatedEndpointCorsHandler: CorsHandler,
    @Named(DependencyInjectionConstants.PUBLIC_CORS)
    private val publicEndpointCorsHandler: CorsHandler,
) {
    private val streamMetadata = JsonObject(
        """{
            "donation_goal": null,
            "current_game_id": null,
            "donatebar_info": [],
            "counters": []
        }"""
    )

    @Suppress("SwallowedException") // Not swallowed, the message is used in augmented error
    fun configure(router: Router) {
        router.route("/stream-metadata").handler { ctx ->
            ctx.response().putHeader("content-type", "application/json")
            ctx.next()
        }
        router.get("/stream-metadata")
            .handler(publicEndpointCorsHandler)
            .handler { ctx ->
                db.get()
                    .onSuccess {
                        ctx.response()
                            .end(jacksonObjectMapper().writeValueAsString(StreamMetadatApiModel.from(it)))
                    }.onFailure(ctx::fail)
            }
        router.patch("/stream-metadata")
            .handler(BodyHandler.create())
            .handler(authenticatedEndpointCorsHandler)
            .handler(authenticationHandler)
            .handler { ctx ->
                db.get()
                    .flatMap { existingMetadata ->
                        val body = try {
                            ctx.bodyAsJson
                        } catch (e: DecodeException) {
                            throw UserError("Invalid json ${e.message}")
                        }
                        body.fieldNames().forEach { key ->
                            if (!streamMetadata.containsKey(key)) {
                                throw UserError("Unknown key in request: $key")
                            }
                        }

                        val newData = try {
                            updateFromJson(existingMetadata, ctx.bodyAsJson)
                        } catch (e: InvalidFormatException) {
                            throw UserError("Invalid request: ${e.message}")
                        }
                        return@flatMap future { p: Promise<StreamMetadata> ->
                            db.save(newData).onSuccess {
                                p.complete(newData)
                            }.onFailure(p::fail)
                        }
                    }
                    .onSuccess {
                        ctx.response()
                            .end(jacksonObjectMapper().writeValueAsString(StreamMetadatApiModel.from(it)))
                    }.onFailure(ctx::fail)
            }
    }

    private fun updateFromJson(data: StreamMetadata, json: JsonObject): StreamMetadata {
        val oldData = jacksonObjectMapper().readerForUpdating(StreamMetadatApiModel.from(data))
        val mergedData: StreamMetadatApiModel = oldData.readValue(json.encode())
        return StreamMetadata(
            mergedData.donationGoal,
            mergedData.currentGameId,
            mergedData.donatebarInfo,
            mergedData.counters
        )
    }
}
