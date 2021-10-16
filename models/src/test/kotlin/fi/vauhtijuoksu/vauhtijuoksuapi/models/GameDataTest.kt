package fi.vauhtijuoksu.vauhtijuoksuapi.models

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vertx.core.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID

internal class GameDataTest {
    private val uuid = UUID.randomUUID()

    // Slight variations in input, within ISO 8601
    private val start = "2021-09-21T15:05:47Z"
    private val end = "2021-09-21T16:05:47-01:00"

    // Accept whatever Date outputs by default, as it's compliant,
    // and we don't care about the exact format further than that
    private val startUtc = "2021-09-21T15:05:47.000+00:00"
    private val endUtc = "2021-09-21T17:05:47.000+00:00"

    private val expectedGameData = JsonObject()
        .put("id", uuid.toString())
        .put("game", "Tetris")
        .put("player", "jsloth")
        .put("start_time", startUtc)
        .put("end_time", endUtc)
        .put("category", "any%")
        .put("device", "PC")
        .put("published", "1970")
        .put("vod_link", "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        .put("img_filename", "tetris.png")
        .put("player_twitch", "jiisloth")

    @Test
    fun testGameDataSerialization() {
        val gameData = GameData(
            uuid,
            "Tetris",
            "jsloth",
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(start))),
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(end))),
            "any%",
            "PC",
            "1970",
            URL("https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
            "tetris.png",
            "jiisloth"
        )

        val expectedGameData = JsonObject()
            .put("id", uuid.toString())
            .put("game", "Tetris")
            .put("player", "jsloth")
            .put("start_time", startUtc)
            .put("end_time", endUtc)
            .put("category", "any%")
            .put("device", "PC")
            .put("published", "1970")
            .put("vod_link", "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
            .put("img_filename", "tetris.png")
            .put("player_twitch", "jiisloth")

        val gameDataAsJson = JsonObject(jacksonObjectMapper().writeValueAsString(gameData))
        assertEquals(expectedGameData, gameDataAsJson)
    }

    @Test
    fun testGameDataSerializationNoVodLink() {
        val gameData = GameData(
            uuid,
            "Tetris",
            "jsloth",
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(start))),
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(end))),
            "any%",
            "PC",
            "1970",
            null,
            "tetris.png",
            "jiisloth"
        )

        val expectedGameData = JsonObject()
            .put("id", uuid.toString())
            .put("game", "Tetris")
            .put("player", "jsloth")
            .put("start_time", startUtc)
            .put("end_time", endUtc)
            .put("category", "any%")
            .put("device", "PC")
            .put("published", "1970")
            .put("img_filename", "tetris.png")
            .put("player_twitch", "jiisloth")

        val gameDataAsJson = JsonObject(jacksonObjectMapper().writeValueAsString(gameData))
        assertEquals(expectedGameData, gameDataAsJson)
    }
}
