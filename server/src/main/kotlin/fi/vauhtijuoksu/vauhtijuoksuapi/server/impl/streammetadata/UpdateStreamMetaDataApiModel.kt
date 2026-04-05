package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.streammetadata

import apimodels.StreamMetadataUpdate
import fi.vauhtijuoksu.vauhtijuoksuapi.models.StreamMetadata
import io.vertx.core.json.JsonObject

fun StreamMetadataUpdate.Companion.from(data: StreamMetadata): StreamMetadataUpdate {
    return StreamMetadataUpdate(
        donationGoal = data.donationGoal,
        currentGameId = data.currentGameId,
        donatebarInfo = data.donateBarInfo,
        counters = data.counters,
        heartRates = data.heartRates,
        nowPlaying = data.nowPlaying,
    )
}

fun StreamMetadataUpdate.toJson(): JsonObject {
    return JsonObject.mapFrom(this)
}
