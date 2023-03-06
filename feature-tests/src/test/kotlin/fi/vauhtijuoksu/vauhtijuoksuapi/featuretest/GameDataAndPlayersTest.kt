package fi.vauhtijuoksu.vauhtijuoksuapi.featuretest

import io.vertx.core.CompositeFuture
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.util.UUID

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@FeatureTest
class GameDataAndPlayersTest {
    private lateinit var client: WebClient

    private val playerData1 = """
        {
          "display_name": "jiisloth",
          "twitch_channel": "jeesloth",
          "discord_nick": "jooosloth#1234"
        }
    """

    private val playerData2 = """
        {
          "display_name": "hluposti",
          "twitch_channel": "kustipolkee",
          "discord_nick": "poosi#8080"
        }
    """

    private val gameData1 = """
        {            
            "game": "Tetris1",
            "start_time": "2021-09-21T15:05:47.000+00:00",
            "end_time": "2021-09-21T16:05:47.000+00:00",
            "category": "100%",
            "device": "Switch",
            "published": "2003",
            "vod_link": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "img_filename": "tetris.png",
            "meta": "k18"
        }
    """

    private val gameData2 = """
        {            
            "game": "Tetris2",
            "start_time": "2021-09-21T15:05:47.000+00:00",
            "end_time": "2021-09-21T16:05:47.000+00:00",
            "category": "any%",
            "device": "PC",
            "published": "1970",
            "vod_link": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "img_filename": "tetris2.png",
            "meta": "Kato tää :D"
        }
    """

    companion object {
        private lateinit var player1Id: UUID
        private lateinit var player2Id: UUID
        private lateinit var game1Id: UUID
        private lateinit var game2Id: UUID
    }

    @BeforeEach
    fun setup(webClient: WebClient) {
        client = webClient
    }

    @Test
    @Order(1)
    fun `add a couple of players`(testContext: VertxTestContext) {
        CompositeFuture.all(
            client.post("/players")
                .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
                .sendJson(JsonObject(playerData1))
                .map {
                    val resJson = it.bodyAsJsonObject()
                    testContext.verify {
                        assertEquals(201, it.statusCode())
                        val sentData = JsonObject(playerData1)
                        sentData.put("id", resJson.getString("id"))
                        assertEquals(sentData, resJson)
                    }
                    player1Id = UUID.fromString(resJson.getString("id"))
                },
            client.post("/players")
                .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
                .sendJson(JsonObject(playerData2))
                .map {
                    testContext.verify {
                        assertEquals(201, it.statusCode())
                    }
                    player2Id = UUID.fromString(it.bodyAsJsonObject().getString("id"))
                },
        )
            .map { testContext.completeNow() }
            .onFailure(testContext::failNow)
    }

    @Test
    @Order(2)
    fun `create new games`(testContext: VertxTestContext) {
        CompositeFuture.all(
            client.post("/gamedata")
                .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
                .sendJson(JsonObject(gameData1).put("players", listOf(player1Id, player2Id)))
                .map { res ->
                    val resJson = res.bodyAsJsonObject()
                    testContext.verify {
                        assertEquals(201, res.statusCode())
                        val original = JsonObject(gameData1)
                            .put("id", resJson.getString("id"))
                            .put("players", listOf(player1Id.toString(), player2Id.toString()))
                        assertEquals(original, res.bodyAsJsonObject())
                    }
                    game1Id = UUID.fromString(res.bodyAsJsonObject().getString("id"))
                },
            client.post("/gamedata")
                .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
                .sendJson(JsonObject(gameData2).put("players", listOf(player2Id)))
                .map {
                    game2Id = UUID.fromString(it.bodyAsJsonObject().getString("id"))
                },
        )
            .map { testContext.completeNow() }
            .onFailure(testContext::failNow)
    }

    @Test
    @Order(3)
    fun `change a game`(testContext: VertxTestContext) {
        client.patch("/gamedata/$game1Id")
            .putHeader("Origin", "http://api.localhost")
            .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
            .sendJson(
                JsonObject(gameData1)
                    .put("game", "Pacman")
                    .put("players", listOf(player1Id)),
            )
            .map {
                testContext.verify {
                    assertEquals(200, it.statusCode())
                    val expectedData = JsonObject(gameData1)
                        .put("id", game1Id.toString())
                        .put("game", "Pacman")
                        .put("players", listOf(player1Id.toString()))
                    assertEquals(expectedData, it.bodyAsJsonObject())
                }
                testContext.completeNow()
            }
            .onFailure(testContext::failNow)
    }

    @Test
    @Order(4)
    fun `remove a game`(testContext: VertxTestContext) {
        client.delete("/gamedata/$game1Id")
            .putHeader("Origin", "http://api.localhost")
            .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
            .send()
            .map {
                testContext.verify {
                    assertEquals(204, it.statusCode())
                }
            }
            .compose {
                client.get("/gamedata/$game1Id")
                    .send()
            }
            .map {
                testContext.verify {
                    assertEquals(404, it.statusCode())
                }
            }
            .compose {
                client.get("/players/$player1Id")
                    .send()
            }
            .map {
                testContext.verify {
                    assertEquals(200, it.statusCode())
                }
                testContext.completeNow()
            }
            .onFailure(testContext::failNow)
    }

    @Test
    @Order(5)
    fun `remove a player`(testContext: VertxTestContext) {
        client.delete("/players/$player2Id")
            .putHeader("Origin", "http://api.localhost")
            .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
            .send()
            .map {
                testContext.verify {
                    assertEquals(204, it.statusCode())
                }
            }
            .compose {
                client.get("/players/$player2Id")
                    .send()
            }
            .map {
                testContext.verify {
                    assertEquals(404, it.statusCode())
                }
            }
            .compose {
                client.get("/gamedata/$game2Id")
                    .send()
            }
            .map {
                testContext.verify {
                    assertEquals(200, it.statusCode())
                    val expectedResponse = JsonObject(gameData2)
                        .put("id", game2Id.toString())
                        .put("players", listOf<String>())
                    assertEquals(expectedResponse, it.bodyAsJsonObject())
                }
                testContext.completeNow()
            }
            .onFailure(testContext::failNow)
    }
}
