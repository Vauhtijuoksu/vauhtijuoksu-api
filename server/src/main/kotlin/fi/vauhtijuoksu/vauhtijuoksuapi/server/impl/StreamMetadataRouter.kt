package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.SingletonDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.StreamMetadata
import fi.vauhtijuoksu.vauhtijuoksuapi.server.ApiConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.model.StreamMetadatApiModel
import io.vertx.core.json.DecodeException
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import mu.KotlinLogging
import javax.inject.Inject
import javax.inject.Named

class StreamMetadataRouter
@Inject constructor(
    private val db: SingletonDatabase<StreamMetadata>,
    private val authenticationHandler: AuthenticationHandler,
    @Named(DependencyInjectionConstants.AUTHENTICATED_CORS)
    private val authenticatedEndpointCorsHandler: CorsHandler,
    @Named(DependencyInjectionConstants.PUBLIC_CORS)
    private val publicEndpointCorsHandler: CorsHandler,
) {
    private val logger = KotlinLogging.logger {}

    private val streamMetadata = JsonObject(
        """{
            "donation_goal": null,
            "current_game_id": null,
            "donatebar_info": [],
            "counters": []
        }"""
    )

    fun configure(router: Router) {
        router.route("/stream-metadata").handler { ctx ->
            ctx.response().putHeader("content-type", "application/json")
            ctx.next()
        }
        router.get("/stream-metadata")
            .handler(publicEndpointCorsHandler)
            .handler { ctx ->
                db.get().onFailure(ctx::fail)
                    .onSuccess {
                        ctx.response()
                            .end(jacksonObjectMapper().writeValueAsString(StreamMetadatApiModel.from(it)))
                    }
            }
        router.patch("/stream-metadata")
            .handler(BodyHandler.create())
            .handler(authenticatedEndpointCorsHandler)
            .handler(authenticationHandler)
            .handler { ctx ->
                db.get()
                    .onFailure(ctx::fail)
                    .onSuccess { existingMetadata ->
                        fun updateMetadata(data: StreamMetadata, json: JsonObject): StreamMetadata {
                            val oldData = jacksonObjectMapper().readerForUpdating(StreamMetadatApiModel.from(data))
                            val mergedData: StreamMetadatApiModel = oldData.readValue(json.encode())
                            return StreamMetadata(
                                mergedData.donationGoal,
                                mergedData.currentGameId,
                                mergedData.donatebarInfo,
                                mergedData.counters
                            )
                        }

                        val body = try {
                            ctx.bodyAsJson
                        } catch (e: DecodeException) {
                            logger.debug { "Invalid json ${e.message}" }
                            ctx.response().setStatusCode(ApiConstants.BAD_REQUEST).end()
                            return@onSuccess
                        }
                        body.fieldNames().forEach { key ->
                            if (!streamMetadata.containsKey(key)) {
                                ctx.response().setStatusCode(ApiConstants.BAD_REQUEST).end()
                                logger.debug { "Unknown key in request: $key" }
                                return@onSuccess
                            }
                        }

                        val newData = try {
                            updateMetadata(existingMetadata, ctx.bodyAsJson)
                        } catch (e: InvalidFormatException) {
                            ctx.response().setStatusCode(ApiConstants.BAD_REQUEST).end()
                            logger.debug { "Invalid request: ${e.message}" }
                            return@onSuccess
                        }
                        db.save(newData)
                            .onFailure(ctx::fail)
                            .onSuccess {
                                ctx.response().end(jacksonObjectMapper().writeValueAsString(StreamMetadatApiModel.from(newData)))
                            }
                    }
            }
    }
}
