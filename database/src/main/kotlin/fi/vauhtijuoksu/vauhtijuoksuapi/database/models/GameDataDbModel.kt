package fi.vauhtijuoksu.vauhtijuoksuapi.database.models

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameParticipant
import fi.vauhtijuoksu.vauhtijuoksuapi.models.ParticipantRole
import java.net.URL
import java.util.Date
import java.util.UUID

@Suppress("ConstructorParameterNaming") // This is cleaner than annotations
data class GameParticipantDbModel(
    val game_id: UUID,
    val participant_id: UUID,
    val role_in_game: ParticipantRole,
    val participant_order: Int,
)

data class GameDataDbModel(
    val id: UUID,
    val game: String,
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
    val meta: String?,
    val participants: List<GameParticipantDbModel>,
) {
    companion object {
        fun fromGameData(gameData: GameData): GameDataDbModel =
            GameDataDbModel(
                gameData.id,
                gameData.game,
                gameData.startTime,
                gameData.endTime,
                gameData.category,
                gameData.device,
                gameData.published,
                gameData.vodLink,
                gameData.imgFilename,
                gameData.meta,
                listOf(),
            )
    }

    fun toGameData(): GameData =
        GameData(
            id,
            game,
            startTime,
            endTime,
            category,
            device,
            published,
            vodLink,
            imgFilename,
            meta,
            participants.map { GameParticipant(it.participant_id, it.role_in_game) },
        )
}
