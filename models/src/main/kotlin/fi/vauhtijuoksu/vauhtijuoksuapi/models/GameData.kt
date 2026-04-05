package fi.vauhtijuoksu.vauhtijuoksuapi.models

import java.util.Date
import java.util.UUID

enum class ParticipantRole {
    PLAYER,
    COUCH,
}

data class GameParticipant(
    val participantId: UUID,
    val role: ParticipantRole,
)

data class GameData(
    override val id: UUID,
    val game: String,
    val startTime: Date,
    val endTime: Date,
    val category: String,
    val device: String,
    val published: String,
    val vodLink: String?,
    val imgFilename: String?,
    val meta: String?,
    val participants: List<GameParticipant>,
) : Model
