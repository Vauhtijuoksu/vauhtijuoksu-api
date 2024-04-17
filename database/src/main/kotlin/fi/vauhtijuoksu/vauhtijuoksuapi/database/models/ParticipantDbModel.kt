package fi.vauhtijuoksu.vauhtijuoksuapi.database.models

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Participant
import fi.vauhtijuoksu.vauhtijuoksuapi.models.SocialMedia
import java.util.UUID

@Suppress("ConstructorParameterNaming")
data class ParticipantDbModel(
    val id: UUID,
    val display_name: String,
    val social_medias: Array<SocialMedia>,
) {
    fun toParticipant(): Participant {
        return Participant(
            id,
            display_name,
            social_medias.toList(),
        )
    }

    companion object {
        fun fromParticipant(participant: Participant): ParticipantDbModel {
            return ParticipantDbModel(
                participant.id,
                participant.displayName,
                participant.socialMedias.toTypedArray(),
            )
        }
    }
}
