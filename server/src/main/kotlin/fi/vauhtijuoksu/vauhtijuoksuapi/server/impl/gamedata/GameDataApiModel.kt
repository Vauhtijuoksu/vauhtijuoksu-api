package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.gamedata

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiModel
import io.vertx.core.json.JsonObject
import java.net.URL
import java.util.Date
import java.util.UUID

data class GameDataApiModel(
    val id: UUID,
    val game: String,
    val player: String,
    @JsonProperty("start_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val startTime: Date,
    @JsonProperty("end_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val endTime: Date,
    val category: String,
    val device: String,
    val published: String,
    @JsonProperty("vod_link")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val vodLink: URL?,
    @JsonProperty("img_filename")
    val imgFilename: String?,
    @JsonProperty("player_twitch")
    val playerTwitch: String?,
) : ApiModel<GameData> {
    companion object {
        fun fromGameData(gameData: GameData): GameDataApiModel {
            return GameDataApiModel(
                gameData.id,
                gameData.game,
                gameData.player,
                gameData.startTime,
                gameData.endTime,
                gameData.category,
                gameData.device,
                gameData.published,
                gameData.vodLink,
                gameData.imgFilename,
                gameData.playerTwitch
            )
        }
    }

    override fun toJson(): JsonObject {
        return JsonObject(jacksonObjectMapper().writeValueAsString(this))
    }

    override fun toModel(): GameData {
        return GameData(
            id,
            game,
            player,
            startTime,
            endTime,
            category,
            device,
            published,
            vodLink,
            imgFilename,
            playerTwitch
        )
    }
}
