package fi.vauhtijuoksu.vauhtijuoksuapi.featuretest

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class GameDataTest {
    lateinit var client: WebClient

    private val gameData1 = """
        {            
            "game": "Tetris2",
            "player": "jsloth",
            "start_time": "2021-09-21T15:05:47.000+00:00",
            "end_time": "2021-09-21T16:05:47.000+00:00",
            "category": "any%",
            "device": "PC",
            "published": "1970",
            "vod_link": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "img_filename": "tetris.png",
            "player_twitch": "jiisloth",
            "meta": "k18"
        }
    """.trimIndent()

    @BeforeEach
    fun setup() {
        client = WebClient.create(Vertx.vertx())
    }

    @Test
    fun `test post and patch`(testContext: VertxTestContext) {
        var id: String = ""
        client.post("/gamedata")
            .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
            .sendJson(JsonObject(gameData1))
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                val resJson = res.bodyAsJsonObject()
                testContext.verify {
                    assertEquals(201, res.statusCode())
                    val original = JsonObject(gameData1)
                    original.put("id", resJson.getString("id"))
                    assertEquals(original, res.bodyAsJsonObject())
                }
            }
            .compose { res ->
                id = res.bodyAsJsonObject().getString("id")
                client.patch("/gamedata/$id")
                    .putHeader("Origin", "http://localhost")
                    .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
                    .sendJson(
                        JsonObject(gameData1)
                            .put("id", id)
                            .put("game", "Tetris3")
                    )
            }
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    val expectedData = JsonObject(gameData1)
                        .put("id", id)
                        .put("game", "Tetris3")
                    assertEquals(200, res.statusCode())
                    assertEquals(expectedData, res.bodyAsJsonObject())
                }
                testContext.completeNow()
            }
    }
}
