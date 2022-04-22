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
        private const val start = "2021-09-21T15:05:47Z"
        private const val end = "2021-09-21T16:05:47-01:00"

        val gameData1 = GameData(
            UUID.randomUUID(),
            "Tetris",
            "jsloth",
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(start))),
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(end))),
            "any%",
            "PC",
            "1970",
            URL("https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
            "tetris.png",
            "jiisloth",
            "k18"
        )

        val gameData2 = GameData(
            UUID.randomUUID(),
            "Hotline Miami",
            "Glukoosi",
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(start))),
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(end))),
            "100%",
            "Potato",
            "1971",
            URL("https://www.youtube.com/watch?v=this is glukoosi"),
            "chicken.png",
            "Glukoosi",
            "kid friendly"
        )

        val gameData3 = GameData(
            UUID.randomUUID(),
            "Halo trilogy",
            "T3mu & Spike_B",
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(start))),
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(end))),
            "toohard%",
            "PC",
            "2004",
            null,
            "chief.png",
            "T3mu & Spike_B",
            "space themed"
        )
    }
}
