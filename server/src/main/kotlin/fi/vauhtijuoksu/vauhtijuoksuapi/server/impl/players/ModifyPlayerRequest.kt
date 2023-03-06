package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.players

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Size

data class ModifyPlayerRequest(
    @Size(min = 1)
    @JsonProperty("display_name")
    val displayName: String?,
    @JsonProperty("twitch_channel")
    val twitchChannel: String?,
    @JsonProperty("discord_nick")
    val discordNick: String?,
)
