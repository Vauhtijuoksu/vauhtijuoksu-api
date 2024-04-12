package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.streammetadata

import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.StreamMetadata
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.timers.TimerApiModel
import io.vertx.core.json.JsonObject
import java.time.OffsetDateTime
import java.util.UUID

internal data class StreamMetaDataApiModel(
    @JsonProperty("donation_goal")
    val donationGoal: Int?,
    @JsonProperty("current_game_id")
    val currentGameId: UUID?,
    @JsonProperty("donatebar_info")
    val donatebarInfo: List<String>,
    @JsonProperty("counters")
    val counters: List<Int>,
    @JsonProperty("heart_rates")
    val heartRates: List<Int>,
    @JsonProperty("timers")
    val timers: List<TimerApiModel>,
    @JsonProperty("now_playing")
    val nowPlaying: String?,
    @JsonProperty("server_time")
    val serverTime: OffsetDateTime,
) {
    companion object {
        fun from(data: StreamMetadata, timers: List<Timer>, now: OffsetDateTime): StreamMetaDataApiModel {
            return StreamMetaDataApiModel(
                data.donationGoal,
                data.currentGameId,
                data.donateBarInfo,
                data.counters,
                data.heartRates,
                timers.map {
                    TimerApiModel.from(it)
                },
                data.nowPlaying,
                now,
            )
        }
    }

    fun toJson(): JsonObject {
        return JsonObject.mapFrom(this)
    }
}
