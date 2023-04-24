package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import fi.vauhtijuoksu.vauhtijuoksuapi.database.impl.MetadataTimerDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.UserError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.StreamMetadata
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.model.StreamMetaDataApiModel
import fi.vauhtijuoksu.vauhtijuoksuapi.server.model.TimerApiModel
import io.vertx.core.Future.future
import io.vertx.core.Promise
import io.vertx.core.json.DecodeException
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import mu.KotlinLogging
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

// Throwing as soon as some validation fails is the simplest way
@Suppress("ThrowsCount")
class StreamMetadataRouter
@Inject constructor(
    private val db: MetadataTimerDatabase,
    private val authenticationHandler: AuthenticationHandler,
    @Named(DependencyInjectionConstants.AUTHENTICATED_CORS)
    private val authenticatedEndpointCorsHandler: CorsHandler,
    @Named(DependencyInjectionConstants.PUBLIC_CORS)
    private val publicEndpointCorsHandler: CorsHandler,
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

    @Suppress("SwallowedException") // Not swallowed, the message is used in augmented error
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
                    .onSuccess { res ->
                        ctx.response()
                            .end(StreamMetaDataApiModel.from(res).toJson().encode())
                    }.onFailure(ctx::fail)
            }
    }

    @Suppress("SwallowedException")
    private fun handlePatch(router: Router) {
        router.patch("/stream-metadata")
            .handler(authenticatedEndpointCorsHandler)
            .handler(BodyHandler.create())
            .handler(authenticationHandler)
            .handler { ctx ->
                db.get()
                    .flatMap { res ->
                        val body = try {
                            ctx.body().asJsonObject()
                        } catch (e: DecodeException) {
                            throw UserError("Invalid json ${e.message}")
                        }
                        val newTimers = handleTimers(body, res.timers)
                        logger.info("Update stream from json $newTimers")
                        val newData = try {
                            updateStreamFromJson(res, ctx.body().asJsonObject())
                        } catch (e: InvalidFormatException) {
                            throw UserError("Invalid request: ${e.message}")
                        }
                        newData.timers = newTimers
                        logger.info("Save to database")
                        return@flatMap future { p: Promise<StreamMetadata> ->
                            db.save(newData)
                                .compose { db.get() }
                                .onSuccess { resNew ->
                                    p.complete(resNew)
                                }
                                .onFailure(p::fail)
                        }
                    }
                    .onSuccess {
                        ctx.response().end(StreamMetaDataApiModel.from(it).toJson().encode())
                    }.onFailure(ctx::fail)
            }
    }

    @Suppress("SwallowedException")
    private fun handleTimers(body: JsonObject, existingTimers: List<Timer>): MutableList<Timer> {
        val newTimers: MutableList<Timer> = mutableListOf()
        logger.info("Handling timers")
        body.fieldNames().forEach { key ->
            if (!streamMetadata.containsKey(key)) {
                throw UserError("Unknown key in request: $key")
            }
        }
        val timerBody = body.getJsonArray("timers")
        if (timerBody != null) {
            val updatedTimers: MutableList<Timer> = mutableListOf()
            for (item in timerBody) {
                val jsonTimer = item as JsonObject
                val index = jsonTimer.getInteger("indexcol")
                val timer = existingTimers.find { it.indexcol == index }
                if (timer != null) {
                    newTimers.add(updateTimersFromJson(timer, item))
                } else {
                    val startTime = jsonTimer.getString("start_time")
                    val endTime = jsonTimer.getString("end_time")
                    var index = jsonTimer.getInteger("indexcol")
                    logger.info("Creating new timer")
                    newTimers.add(
                        Timer(
                            UUID.randomUUID(),
                            if (startTime != null) {
                                OffsetDateTime.ofInstant(
                                    Instant.from(DateTimeFormatter.ISO_INSTANT.parse(startTime)),
                                    ZoneId.of("Z"),
                                )
                            } else {
                                null
                            },
                            if (endTime != null) {
                                OffsetDateTime.ofInstant(
                                    Instant.from(DateTimeFormatter.ISO_INSTANT.parse(endTime)),
                                    ZoneId.of("Z"),
                                )
                            } else {
                                null
                            },
                            index,
                        ),
                    )
                }
            }
            newTimers.addAll(0, updatedTimers)
        }
        return newTimers
    }

    private fun updateStreamFromJson(data: StreamMetadata, json: JsonObject): StreamMetadata {
        val oldData = DatabindCodec.mapper().readerForUpdating(StreamMetaDataApiModel.from(data))
        val mergedData: StreamMetaDataApiModel = oldData.readValue(json.encode())
        return StreamMetadata(
            mergedData.donationGoal,
            mergedData.currentGameId,
            mergedData.donatebarInfo,
            mergedData.counters,
            mergedData.heartRates,
            listOf(),
            mergedData.nowPlaying,
        )
    }

    private fun updateTimersFromJson(data: Timer, json: JsonObject): Timer {
        val oldData = DatabindCodec.mapper().readerForUpdating(TimerApiModel.from(data))
        val mergedData: TimerApiModel = oldData.readValue(json.encode())
        return Timer(
            data.id,
            mergedData.startTime,
            mergedData.endTime,
            mergedData.indexcol,
        )
    }
}
