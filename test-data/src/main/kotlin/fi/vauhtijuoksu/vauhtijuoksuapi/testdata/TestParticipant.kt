package fi.vauhtijuoksu.vauhtijuoksuapi.testdata

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Participant
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Platform
import fi.vauhtijuoksu.vauhtijuoksuapi.models.SocialMedia
import java.util.UUID

class TestParticipant private constructor() {
    companion object {
        val participant1 =
            Participant(
                UUID.randomUUID(),
                "hluposti",
                listOf(),
            )

        val participant2 =
            Participant(
                UUID.randomUUID(),
                "jiisloth",
                listOf(
                    SocialMedia(Platform.TWITCH, "slotti"),
                    SocialMedia(Platform.DISCORD, "slottivaan"),
                ),
            )

        val participant3 =
            Participant(
                UUID.randomUUID(),
                "runtu",
                listOf(
                    SocialMedia(Platform.TWITCH, "runtelii"),
                    SocialMedia(Platform.DISCORD, "märkylii"),
                ),
            )
    }
}
