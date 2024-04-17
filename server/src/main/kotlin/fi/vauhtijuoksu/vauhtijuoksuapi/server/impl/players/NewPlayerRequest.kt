package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.players

import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Participant
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Platform
import fi.vauhtijuoksu.vauhtijuoksuapi.models.SocialMedia
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
    fun toParticipant(id: UUID): Participant {
        return Participant(
            id,
            displayName,
            listOfNotNull(
                if (twitchChannel != null) SocialMedia(Platform.TWITCH, twitchChannel) else null,
                if (discordNick != null) SocialMedia(Platform.DISCORD, discordNick) else null,
            ),
        )
    }
}
