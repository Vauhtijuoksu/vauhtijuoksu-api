package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.players

import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Player
import jakarta.validation.constraints.Size
import java.util.UUID

data class NewPlayerRequest(
    @Size(min = 1)
    @JsonProperty("display_name")
    val displayName: String,
    @JsonProperty("twitch_channel")
    val twitchChannel: String?,
    @JsonProperty("discord_nick")
    val discordNick: String?,
) {
    fun toPlayer(id: UUID): Player {
        return Player(
            id,
            displayName,
            twitchChannel,
            discordNick,
        )
    }
}
