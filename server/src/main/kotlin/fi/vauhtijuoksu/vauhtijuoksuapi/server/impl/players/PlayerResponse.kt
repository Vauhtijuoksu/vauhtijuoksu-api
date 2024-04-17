package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.players

import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Participant
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Platform
import fi.vauhtijuoksu.vauhtijuoksuapi.models.SocialMedia
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiModel
import io.vertx.core.json.JsonObject
import java.util.UUID

data class PlayerResponse(
    val id: UUID,
    @JsonProperty("display_name")
    val displayName: String,
    @JsonProperty("twitch_channel")
    val twitchChannel: String?,
    @JsonProperty("discord_nick")
    val discordNick: String?,
) : ApiModel<Participant> {
    companion object {
        fun fromParticipant(participant: Participant): PlayerResponse {
            return PlayerResponse(
                participant.id,
                participant.displayName,
                participant.socialMedias.find { it.platform == Platform.TWITCH }?.username,
                participant.socialMedias.find { it.platform == Platform.DISCORD }?.username,
            )
        }
    }

    override fun toModel(): Participant {
        return Participant(
            id,
            displayName,
            listOfNotNull(
                if (twitchChannel != null) SocialMedia(Platform.TWITCH, twitchChannel) else null,
                if (discordNick != null) SocialMedia(Platform.DISCORD, discordNick) else null,
            ),
        )
    }

    override fun toJson(): JsonObject {
        return JsonObject.mapFrom(this)
    }
}
