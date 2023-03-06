package fi.vauhtijuoksu.vauhtijuoksuapi.database.models

import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Player
import java.util.*

data class PlayerDbModel(
    val id: UUID,
    @JsonProperty("display_name")
    val displayName: String,
    @JsonProperty("twitch_channel")
    val twitchChannel: String?,
    @JsonProperty("discord_nick")
    val discordNick: String?,
) {
    fun toPlayer(): Player {
        return Player(
            id,
            displayName,
            twitchChannel,
            discordNick,
        )
    }

    companion object {
        fun fromPlayer(player: Player): PlayerDbModel {
            return PlayerDbModel(
                player.id,
                player.displayName,
                player.twitchChannel,
                player.discordNick,
            )
        }
    }
}
