package fi.vauhtijuoksu.vauhtijuoksuapi.database.models

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import java.net.URL
import java.util.Date
import java.util.UUID

data class GameDataDbModel(
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
) {
    companion object {
        fun fromGameData(gameData: GameData): GameDataDbModel {
            return GameDataDbModel(
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
                gameData.playerTwitch,
            )
        }
    }

    fun toGameData(): GameData {
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
            playerTwitch,
        )
    }
}
