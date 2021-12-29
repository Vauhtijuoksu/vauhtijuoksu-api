package fi.vauhtijuoksu.vauhtijuoksuapi.database.models

import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.StreamMetadata
import java.util.UUID

internal data class StreamMetadataDbModel(
    @Suppress("UnusedPrivateMember") // Simplifies automatic mapping
    private val id: Boolean = true,
    @JsonProperty("donation_goal")
    val donationGoal: Int?,
    @JsonProperty("current_game_id")
    val currentGameId: UUID?,
    @JsonProperty("donatebar_info")
    val donatebarInfo: List<String>,
    @JsonProperty("counters")
    val counters: List<Int>
) {
    fun toStreamMetadata(): StreamMetadata {
        return StreamMetadata(
            donationGoal,
            currentGameId,
            donatebarInfo,
            counters
        )
    }
}
