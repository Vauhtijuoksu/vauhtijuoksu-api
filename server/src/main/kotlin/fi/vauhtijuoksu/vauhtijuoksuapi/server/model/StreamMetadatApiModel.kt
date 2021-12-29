package fi.vauhtijuoksu.vauhtijuoksuapi.server.model

import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.StreamMetadata
import java.util.UUID

internal data class StreamMetadatApiModel(
    @JsonProperty("donation_goal")
    val donationGoal: Int?,
    @JsonProperty("current_game_id")
    val currentGameId: UUID?,
    @JsonProperty("donatebar_info")
    val donatebarInfo: List<String>,
    @JsonProperty("counters")
    val counters: List<Int>
) {
    companion object {
        fun from(data: StreamMetadata): StreamMetadatApiModel {
            return StreamMetadatApiModel(data.donationGoal, data.currentGameId, data.donateBarInfo, data.counters)
        }
    }
}
