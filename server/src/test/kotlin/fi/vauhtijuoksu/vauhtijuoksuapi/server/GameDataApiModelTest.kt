package fi.vauhtijuoksu.vauhtijuoksuapi.server

import fi.vauhtijuoksu.vauhtijuoksuapi.models.ParticipantRole
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.gamedata.GameDataApiModel
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.gamedata.GameParticipantApiModel
import io.vertx.core.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID

internal class GameDataApiModelTest {
    private val uuid = UUID.randomUUID()

    // Slight variations in input, within ISO 8601
    private val start = "2021-09-21T15:05:47Z"
    private val end = "2021-09-21T16:05:47-01:00"

    // Accept whatever Date outputs by default, as it's compliant,
    // and we don't care about the exact format further than that
    private val startUtc = "2021-09-21T15:05:47.000+00:00"
    private val endUtc = "2021-09-21T17:05:47.000+00:00"

    private val playerId = UUID.randomUUID()

    private lateinit var expectedGameData: JsonObject

    @BeforeEach
    fun setup() {
        expectedGameData = JsonObject()
            .put("id", uuid.toString())
            .put("game", "Tetris")
            .put("start_time", startUtc)
            .put("end_time", endUtc)
            .put("category", "any%")
            .put("device", "PC")
            .put("published", "1970")
            .put("vod_link", "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
            .put("img_filename", "tetris.png")
            .put("meta", "k18")
            .put("participants", listOf(JsonObject().put("participant_id", playerId.toString()).put("role", "PLAYER")))
    }

    @Test
    fun testGameDataSerialization() {
        val gameData = GameDataApiModel(
            uuid,
            "Tetris",
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(start))),
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(end))),
            "any%",
            "PC",
            "1970",
            URL("https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
            "tetris.png",
            "k18",
            listOf(
                GameParticipantApiModel(
                    playerId,
                    ParticipantRole.PLAYER,
                ),
            ),
        )

        val gameDataAsJson = gameData.toJson()
        assertEquals(expectedGameData, gameDataAsJson)
    }

    @Test
    fun testGameDataSerializationNoVodLink() {
        expectedGameData.remove("vod_link")
        val gameDataApiModel = GameDataApiModel(
            uuid,
            "Tetris",
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(start))),
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(end))),
            "any%",
            "PC",
            "1970",
            null,
            "tetris.png",
            "k18",
            listOf(
                GameParticipantApiModel(
                    playerId,
                    ParticipantRole.PLAYER,
                ),
            ),
        )

        val gameDataAsJson = gameDataApiModel.toJson()
        assertEquals(expectedGameData, gameDataAsJson)
    }
}
