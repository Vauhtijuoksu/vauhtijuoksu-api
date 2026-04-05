package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.streammetadata

import apimodels.StreamMetadataResponse
import apimodels.StreamMetadataUpdate
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.impl.StreamMetadataDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.UserError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.StreamMetadata
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import io.vertx.core.json.DecodeException
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.AuthorizationHandler
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import jakarta.inject.Inject
import jakarta.inject.Named
import mu.KotlinLogging
import java.time.Clock
import kotlin.time.toKotlinInstant

// Throwing as soon as some validation fails is the simplest way
@Suppress("ThrowsCount")
class StreamMetadataRouter
@Inject constructor(
    private val db: StreamMetadataDatabase,
    private val timerDb: VauhtijuoksuDatabase<Timer>,
    private val authenticationHandler: AuthenticationHandler,
    private val adminRequired: AuthorizationHandler,
    @param:Named(DependencyInjectionConstants.Companion.AUTHENTICATED_CORS)
    private val authenticatedEndpointCorsHandler: CorsHandler,
    @param:Named(DependencyInjectionConstants.Companion.PUBLIC_CORS)
    private val publicEndpointCorsHandler: CorsHandler,
    private val clock: Clock,
) {
    private val streamMetadata = JsonObject(
        """{
            "donation_goal": null,
            "now_playing": null,
            "current_game_id": null,
            "donatebar_info": [],
            "counters": [],
            "heart_rates": [],
            "timers": []
        }""",
    )

    private val logger = KotlinLogging.logger {}

    fun configure(router: Router) {
        router.route("/stream-metadata").handler { ctx ->
            ctx.response().putHeader("content-type", "application/json")
            ctx.next()
        }
        handleGet(router)
        handlePatch(router)
    }

    private fun handleGet(router: Router) {
        router.get("/stream-metadata")
            .handler(publicEndpointCorsHandler)
            .handler { ctx ->
                db.get()
                    .flatMap { streamMetadata ->
                        timerDb.getAll().map { timers ->
                            StreamMetadataResponse.from(
                                streamMetadata,
                                timers,
                                clock.instant().toKotlinInstant(),
                            )
                        }
                    }
                    .onSuccess {
                        ctx.response()
                            .end(it.toJson().encode())
                    }.onFailure(ctx::fail)
            }
    }

    @Suppress("SwallowedException")
    private fun handlePatch(router: Router) {
        router.patch("/stream-metadata")
            .handler(authenticatedEndpointCorsHandler)
            .handler(BodyHandler.create())
            .handler(authenticationHandler)
            .handler(adminRequired)
            .handler { ctx ->
                db.get()
                    .flatMap { res ->
                        val body = try {
                            ctx.body().asJsonObject()
                        } catch (e: DecodeException) {
                            throw UserError("Invalid json ${e.message}")
                        }
                        body.fieldNames().forEach { key ->
                            if (!streamMetadata.containsKey(key)) {
                                throw UserError("Unknown key in request: $key")
                            }
                        }
                        val newData = try {
                            updateStreamFromJson(res, body)
                        } catch (e: InvalidFormatException) {
                            throw UserError("Invalid request: ${e.message}")
                        }
                        logger.info("Save to database")
                        db.save(newData)
                    }
                    .flatMap { db.get() }
                    .flatMap { streamMetadata ->
                        timerDb.getAll().map { timers ->
                            StreamMetadataResponse.from(
                                streamMetadata,
                                timers,
                                clock.instant().toKotlinInstant(),
                            )
                        }
                    }
                    .map {
                        ctx.response().end(it.toJson().encode())
                    }
                    .onFailure(ctx::fail)
            }
    }

    private fun updateStreamFromJson(data: StreamMetadata, json: JsonObject): StreamMetadata {
        val oldData = DatabindCodec.mapper().readerForUpdating(StreamMetadataUpdate.from(data))
        val mergedData = oldData.readValue<StreamMetadataUpdate>(json.encode())
        return StreamMetadata(
            donationGoal = mergedData.donationGoal,
            currentGameId = mergedData.currentGameId,
            donateBarInfo = mergedData.donatebarInfo ?: emptyList(),
            counters = mergedData.counters ?: emptyList(),
            heartRates = mergedData.heartRates ?: emptyList(),
            nowPlaying = mergedData.nowPlaying,
        )
    }
}
