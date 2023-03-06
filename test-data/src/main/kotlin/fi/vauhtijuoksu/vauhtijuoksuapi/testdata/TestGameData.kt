package fi.vauhtijuoksu.vauhtijuoksuapi.testdata

import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID

class TestGameData private constructor() {
    companion object {
        // Slight variations in input, within ISO 8601
        private const val early_start = "2021-09-21T15:05:47Z"
        private const val late_start = "2021-09-21T17:05:47Z"
        private const val end = "2021-09-21T16:05:47-01:00"

        val gameData1 = GameData(
            UUID.randomUUID(),
            "Tetris",
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(early_start))),
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(end))),
            "any%",
            "PC",
            "1970",
            URL("https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
            "tetris.png",
            "k18",
            listOf(TestPlayer.player1.id),
        )

        val gameData2 = GameData(
            UUID.randomUUID(),
            "Hotline Miami",
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(late_start))),
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(end))),
            "100%",
            "Potato",
            "1971",
            URL("https://www.youtube.com/watch?v=this is glukoosi"),
            "chicken.png",
            "kid friendly",
            listOf(TestPlayer.player2.id),
        )

        val gameData3 = GameData(
            UUID.randomUUID(),
            "Halo trilogy",
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(early_start))),
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(end))),
            "toohard%",
            "PC",
            "2004",
            null,
            "chief.png",
            "space themed",
            listOf(TestPlayer.player2.id, TestPlayer.player1.id),
        )
    }
}
