package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.gamedata

import apimodels.GameDataCreate
import apimodels.GameDataOutput
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameParticipant
import fi.vauhtijuoksu.vauhtijuoksuapi.models.ParticipantRole
import io.vertx.core.json.JsonObject
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID

fun apimodels.GameParticipant.toGameParticipant(): GameParticipant {
    return GameParticipant(
        participantId = this.id,
        role = when (this.role) {
            apimodels.GameParticipant.Role.PLAYER -> ParticipantRole.PLAYER
            apimodels.GameParticipant.Role.COUCH -> ParticipantRole.COUCH
        },
    )
}

private fun toDate(date: String): Date =
    Date.from(Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(date)))

fun GameDataCreate.toGameData(): GameData {
    return GameData(
        UUID.randomUUID(),
        game = this.game,
        startTime = toDate(this.startTime),
        endTime = toDate(this.endTime),
        category = this.category,
        device = this.device,
        published = this.published,
        vodLink = this.vodLink,
        imgFilename = this.imgFilename,
        meta = this.meta,
        participants = this.participants.map {
            it.toGameParticipant()
        },
    )
}

fun GameDataOutput.Companion.fromGameData(data: GameData): GameDataOutput {
    return GameDataOutput(
        game = data.game,
        participants = data.participants.map {
            apimodels.GameParticipant(
                id = it.participantId,
                role = when (it.role) {
                    ParticipantRole.PLAYER -> apimodels.GameParticipant.Role.PLAYER
                    ParticipantRole.COUCH -> apimodels.GameParticipant.Role.COUCH
                },
            )
        },
        startTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(data.startTime.toInstant()),
        endTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(data.endTime.toInstant()),
        category = data.category,
        device = data.device,
        published = data.published,
        vodLink = data.vodLink,
        imgFilename = data.imgFilename,
        meta = data.meta,
    )
}

fun GameDataOutput.toJson(): JsonObject = JsonObject.mapFrom(this)
