package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.participants

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Participant
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Platform
import fi.vauhtijuoksu.vauhtijuoksuapi.models.SocialMedia
import jakarta.validation.constraints.Size
import java.util.UUID

data class NewSocialMediaRequest(
    val platform: String,
    val username: String,
)

@Suppress("ConstructorParameterNaming")
data class NewParticipantRequest(
    @Size(min = 1)
    val display_name: String,
    val social_medias: List<NewSocialMediaRequest>,
) {
    fun toParticipant(id: UUID): Participant =
        Participant(
            id,
            display_name,
            social_medias.map {
                SocialMedia(
                    Platform.valueOf(it.platform),
                    it.username,
                )
            },
        )
}
