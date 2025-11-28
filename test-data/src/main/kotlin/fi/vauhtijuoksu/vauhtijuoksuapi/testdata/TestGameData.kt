package fi.vauhtijuoksu.vauhtijuoksuapi.testdata

import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameParticipant
import fi.vauhtijuoksu.vauhtijuoksuapi.models.ParticipantRole
import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID

class TestGameData private constructor() {
    companion object {
        // Slight variations in input, within ISO 8601
        private const val EARLY_START = "2021-09-21T15:05:47Z"
        private const val LATE_START = "2021-09-21T17:05:47Z"
        private const val END = "2021-09-21T16:05:47-01:00"

        val gameData1 =
            GameData(
                UUID.randomUUID(),
                "Tetris",
                Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(EARLY_START))),
                Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(END))),
                "any%",
                "PC",
                "1970",
                URL("https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
                "tetris.png",
                "k18",
                listOf(GameParticipant(TestParticipant.participant1.id, ParticipantRole.PLAYER)),
            )

        val gameData2 =
            GameData(
                UUID.randomUUID(),
                "Hotline Miami",
                Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(LATE_START))),
                Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(END))),
                "100%",
                "Potato",
                "1971",
                URL("https://www.youtube.com/watch?v=this is glukoosi"),
                "chicken.png",
                "kid friendly",
                listOf(GameParticipant(TestParticipant.participant2.id, ParticipantRole.PLAYER)),
            )

        val gameData3 =
            GameData(
                UUID.randomUUID(),
                "Halo trilogy",
                Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(EARLY_START))),
                Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(END))),
                "toohard%",
                "PC",
                "2004",
                null,
                "chief.png",
                "space themed",
                listOf(
                    GameParticipant(TestParticipant.participant2.id, ParticipantRole.PLAYER),
                    GameParticipant(TestParticipant.participant1.id, ParticipantRole.PLAYER),
                ),
            )
    }
}
