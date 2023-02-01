package fi.vauhtijuoksu.vauhtijuoksuapi.database.models

import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.PlayerInfo

internal data class PlayerInfoDbModel(
    @Suppress("UnusedPrivateMember") // Simplifies automatic mapping
    private val id: Boolean = true,
    @JsonProperty("message")
    val message: String?,
) {
    fun toPlayerInfo(): PlayerInfo {
        return PlayerInfo(
            message,
        )
    }
}
