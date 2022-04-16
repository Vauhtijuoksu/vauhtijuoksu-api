package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.SingletonDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
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
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import mu.KotlinLogging
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

// Throwing as soon as some validation fails is the simplest way
@Suppress("ThrowsCount")
class StreamMetadataRouter
@Inject constructor(
    private val dbStream: SingletonDatabase<StreamMetadata>,
    private val dbTimer: VauhtijuoksuDatabase<Timer>,
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
            "counters": [],
            "heart_rates": [],
            "timers": []
        }"""
    )
    private val emptyMetaData = StreamMetadata(
        null,
        null,
        listOf(),
        listOf(),
        listOf()
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
                var metaData = emptyMetaData
                dbStream.get()
                    .onSuccess { metaData = it }
                    .onFailure(ctx::fail)
                    .compose { dbTimer.getAll() }
                    .onSuccess { res ->
                        ctx.response()
                            .end(jacksonObjectMapper().writeValueAsString(StreamMetaDataApiModel.from(metaData, res)))
                    }.onFailure(ctx::fail)
            }
    }

    @Suppress("SwallowedException")
    private fun handlePatch(router: Router) {
        router.patch("/stream-metadata")
            .handler(BodyHandler.create())
            .handler(authenticatedEndpointCorsHandler)
            .handler(authenticationHandler)
            .handler { ctx ->
                var metaData = emptyMetaData
                var newTimers: MutableList<Timer> = mutableListOf()
                var oldTimers: List<Timer> = listOf()
                dbStream.get()
                    .onSuccess {
                        metaData = it
                    }.onFailure(ctx::fail)
                    .compose { dbTimer.getAll() }
                    .flatMap { existingTimers ->
                        oldTimers = existingTimers
                        val body = try { ctx.bodyAsJson } catch (e: DecodeException) {
                            throw UserError("Invalid json ${e.message}")
                        }
                        newTimers = handleTimers(body, existingTimers)
                        val newData = try {
                            updateStreamFromJson(metaData, ctx.bodyAsJson, existingTimers)
                        } catch (e: InvalidFormatException) {
                            throw UserError("Invalid request: ${e.message}")
                        }
                        return@flatMap future { p: Promise<StreamMetadata> ->
                            if (newTimers.size > 0) {
                                dbTimer.addAll(newTimers)
                                    .compose { dbStream.save(newData) }
                                    .onSuccess { p.complete(newData) }
                                    .onFailure(p::fail)
                            } else
                                dbStream.save(newData)
                                    .onSuccess { p.complete(newData) }
                                    .onFailure(p::fail)
                        }
                    }
                    .onSuccess {
                        val resTimers = if (newTimers.size > 0) newTimers else oldTimers
                        ctx.response().end(jacksonObjectMapper().writeValueAsString(StreamMetaDataApiModel.from(it, resTimers)))
                    }.onFailure(ctx::fail)
            }
    }

    @Suppress("SwallowedException")
    private fun handleTimers(body: JsonObject, existingTimers: List<Timer>): MutableList<Timer> {
        val newTimers: MutableList<Timer> = mutableListOf()
        body.fieldNames().forEach { key ->
            if (!streamMetadata.containsKey(key)) {
                throw UserError("Unknown key in request: $key")
            }
        }
        val timerBody = body.getJsonArray("timers")
        if (timerBody != null) {
            val timerSize = timerBody.size()
            val updatedTimers = existingTimers.mapIndexed { index, timer ->
                if (index < timerSize)
                    try {
                        updateTimersFromJson(timer, timerBody.getJsonObject(index))
                    } catch (e: InvalidFormatException) {
                        throw UserError("Invalid request: ${e.message}")
                    }
                else
                    timer
            }
            if (timerSize > updatedTimers.size) {
                for (i in updatedTimers.size..timerSize - 1) {
                    val bt = timerBody.getJsonObject(i)
                    val startTime = bt.getString("start_time")
                    val endTime = bt.getString("end_time")
                    newTimers.add(
                        Timer(
                            UUID.randomUUID(),
                            if (startTime != null)
                                Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(startTime))) else null,
                            if (endTime != null)
                                Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(endTime))) else null
                        )
                    )
                }
            }
            newTimers.addAll(0, updatedTimers)
        }
        return newTimers
    }

    private fun updateStreamFromJson(data: StreamMetadata, json: JsonObject, timers: List<Timer>): StreamMetadata {
        val oldData = jacksonObjectMapper().readerForUpdating(StreamMetaDataApiModel.from(data, timers))
        val mergedData: StreamMetaDataApiModel = oldData.readValue(json.encode())
        return StreamMetadata(
            mergedData.donationGoal,
            mergedData.currentGameId,
            mergedData.donatebarInfo,
            mergedData.counters,
            mergedData.heartRates
        )
    }

    private fun updateTimersFromJson(data: Timer, json: JsonObject): Timer {
        val oldData = jacksonObjectMapper().readerForUpdating(TimerApiModel.from(data))
        val mergedData: TimerApiModel = oldData.readValue(json.encode())
        return Timer(
            data.id,
            mergedData.startTime,
            mergedData.endTime
        )
    }
}
