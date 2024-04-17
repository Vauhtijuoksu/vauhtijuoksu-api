package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.gamedata

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameParticipant
import fi.vauhtijuoksu.vauhtijuoksuapi.models.ParticipantRole
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiModel
import io.vertx.core.json.JsonObject
import java.net.URL
import java.util.Date
import java.util.UUID

@Suppress("ConstructorParameterNaming")
data class GameParticipantApiModel(
    val participant_id: UUID,
    val role: ParticipantRole,
)

data class GameDataApiModel(
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
    val players: List<UUID>,
    val participants: List<GameParticipantApiModel>,
) : ApiModel<GameData> {
    companion object {
        fun fromGameData(gameData: GameData): GameDataApiModel {
            return GameDataApiModel(
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
                gameData.participants.filter { it.role == ParticipantRole.PLAYER }.map { it.participantId },
                gameData.participants.map { GameParticipantApiModel(it.participantId, it.role) },
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
            startTime,
            endTime,
            category,
            device,
            published,
            vodLink,
            imgFilename,
            meta,
            players
                .map {
                    GameParticipant(
                        it,
                        ParticipantRole.PLAYER,
                    )
                } + participants
                .filter { it.participant_id !in players }
                .map {
                    GameParticipant(it.participant_id, it.role)
                },
        )
    }
}
