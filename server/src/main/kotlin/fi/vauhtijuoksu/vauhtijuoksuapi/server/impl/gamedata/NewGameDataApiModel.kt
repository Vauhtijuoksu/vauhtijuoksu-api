package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.gamedata

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameParticipant
import fi.vauhtijuoksu.vauhtijuoksuapi.models.ParticipantRole
import java.net.URL
import java.util.Date
import java.util.UUID

data class NewGameDataApiModel(
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
    val participants: List<GameParticipantApiModel> = listOf(),
) {
    fun toGameData(id: UUID): GameData {
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
            players.map {
                GameParticipant(
                    it,
                    ParticipantRole.PLAYER,
                )
            } + participants.filter { it.participant_id !in players }
                .map { GameParticipant(it.participant_id, it.role) },
        )
    }
}
