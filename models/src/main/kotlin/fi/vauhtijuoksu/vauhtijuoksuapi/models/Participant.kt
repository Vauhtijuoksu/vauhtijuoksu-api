package fi.vauhtijuoksu.vauhtijuoksuapi.models

import java.util.UUID

enum class Platform {
    TWITCH,
    DISCORD,
}

data class SocialMedia(
    val platform: Platform,
    val username: String,
) {
    companion object {
        fun twitch(username: String) = SocialMedia(Platform.TWITCH, username)
        fun discord(username: String) = SocialMedia(Platform.DISCORD, username)
    }
}

data class Participant(
    override val id: UUID,
    val displayName: String,
    val socialMedias: List<SocialMedia>,
) : Model
