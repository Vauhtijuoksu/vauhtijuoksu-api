package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.players

import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Player
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
) : ApiModel<Player> {
    companion object {
        fun fromPlayer(player: Player): PlayerResponse {
            return PlayerResponse(
                player.id,
                player.displayName,
                player.twitchChannel,
                player.discordNick,
            )
        }
    }

    override fun toModel(): Player {
        return Player(
            id,
            displayName,
            twitchChannel,
            discordNick,
        )
    }

    override fun toJson(): JsonObject {
        return JsonObject.mapFrom(this)
    }
}
