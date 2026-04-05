package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.streammetadata

import apimodels.StreamMetadataResponse
import apimodels.StreamMetadataResponseTimersInner
import fi.vauhtijuoksu.vauhtijuoksuapi.models.StreamMetadata
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import io.vertx.core.json.JsonObject
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

fun StreamMetadataResponse.Companion.from(
    data: StreamMetadata,
    timers: List<Timer>,
    now: Instant,
): StreamMetadataResponse {
    return StreamMetadataResponse(
        donationGoal = data.donationGoal,
        currentGameId = data.currentGameId,
        nowPlaying = data.nowPlaying,
        donatebarInfo = data.donateBarInfo,
        counters = data.counters,
        heartRates = data.heartRates,
        serverTime = now,
        timers = timers.map {
            StreamMetadataResponseTimersInner.from(it)
        },
    )
}

fun StreamMetadataResponse.toJson(): JsonObject {
    return JsonObject.mapFrom(this)
}

fun StreamMetadataResponseTimersInner.Companion.from(timer: Timer): StreamMetadataResponseTimersInner {
    return StreamMetadataResponseTimersInner(
        timer.id.toString(),
        timer.name,
        timer.startTime?.toInstant()?.toKotlinInstant(),
        timer.endTime?.toInstant()?.toKotlinInstant(),
    )
}
