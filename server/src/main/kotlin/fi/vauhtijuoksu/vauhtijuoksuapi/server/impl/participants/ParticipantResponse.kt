package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.participants

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Participant
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Platform
import fi.vauhtijuoksu.vauhtijuoksuapi.models.SocialMedia
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiModel
import io.vertx.core.json.JsonObject
import java.util.UUID

data class SocialMediaResponse(
    val platform: String,
    val username: String,
)

@Suppress("ConstructorParameterNaming")
data class ParticipantResponse(
    val id: UUID,
    val display_name: String,
    val social_medias: List<SocialMediaResponse>,
) : ApiModel<Participant> {
    companion object {
        fun fromParticipant(participant: Participant): ParticipantResponse {
            return ParticipantResponse(
                participant.id,
                participant.displayName,
                participant.socialMedias.map { SocialMediaResponse(it.platform.name, it.username) },
            )
        }
    }

    override fun toModel(): Participant {
        return Participant(
            id,
            display_name,
            social_medias.map { SocialMedia(Platform.valueOf(it.platform), it.username) },
        )
    }

    override fun toJson(): JsonObject {
        return JsonObject.mapFrom(this)
    }
}
