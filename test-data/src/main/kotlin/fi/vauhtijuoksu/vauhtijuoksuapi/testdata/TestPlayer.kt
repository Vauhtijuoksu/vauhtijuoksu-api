package fi.vauhtijuoksu.vauhtijuoksuapi.testdata

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Player
import java.util.UUID

class TestPlayer private constructor() {
    companion object {
        val player1 = Player(
            UUID.randomUUID(),
            "hluposti",
            "twitch.tv/posti",
            "postnorden",
        )

        val player2 = Player(
            UUID.randomUUID(),
            "jiisloth",
            "twitch.tv/slotti",
            "slottivaan",
        )

        val player3 = Player(
            UUID.randomUUID(),
            "runtu",
            "twitch.tv/runtelii",
            "märkylii",
        )
    }
}
