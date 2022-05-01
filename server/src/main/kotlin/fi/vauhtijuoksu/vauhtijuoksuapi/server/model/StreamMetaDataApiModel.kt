package fi.vauhtijuoksu.vauhtijuoksuapi.server.model

import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.StreamMetadata
import io.vertx.core.json.JsonObject
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
    val nowPlaying: String?
) {
    companion object {
        fun from(data: StreamMetadata): StreamMetaDataApiModel {
            return StreamMetaDataApiModel(
                data.donationGoal,
                data.currentGameId,
                data.donateBarInfo,
                data.counters,
                data.heartRates,
                data.timers.map {
                    TimerApiModel.from(it)
                },
                data.nowPlaying
            )
        }
    }

    fun toJson(): JsonObject {
        return JsonObject.mapFrom(this)
    }
}
