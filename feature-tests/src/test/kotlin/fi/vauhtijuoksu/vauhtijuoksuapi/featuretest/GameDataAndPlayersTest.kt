package fi.vauhtijuoksu.vauhtijuoksuapi.featuretest

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.test.runTest
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
          "social_medias": [
            { "platform": "TWITCH",
              "username": "jeesloth" },
            { "platform": "DISCORD",
              "username": "jooosloth#1234" }
          ]
        }
    """

    private val playerData2 = """
        {
          "display_name": "hluposti",
          "social_medias": [
            { "platform": "TWITCH",
              "username": "kustipolkee" },
            { "platform": "DISCORD",
              "username": "poosi#8080" }
          ]
        }
    """
    private val participantData1 = """
        {
          "display_name": "kustiposti",
          "social_medias": [{
            "platform": "TWITCH",
            "username": "kusti"
          },{
            "platform": "DISCORD",
            "username": "kusti"
          }]
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
        private lateinit var participant1Id: UUID
        private lateinit var game1Id: UUID
        private lateinit var game2Id: UUID
    }

    @BeforeEach
    fun setup(webClient: WebClient) {
        client = webClient
    }

    @Test
    @Order(1)
    fun `add a couple of players`() =
        runTest {
            Future
                .all(
                    client
                        .post("/participants")
                        .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
                        .sendJson(JsonObject(playerData1))
                        .map {
                            val resJson = it.bodyAsJsonObject()
                            assertEquals(201, it.statusCode())
                            val sentData = JsonObject(playerData1)
                            sentData.put("id", resJson.getString("id"))
                            assertEquals(sentData, resJson)
                            player1Id = UUID.fromString(resJson.getString("id"))
                        },
                    client
                        .post("/participants")
                        .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
                        .sendJson(JsonObject(playerData2))
                        .map {
                            assertEquals(201, it.statusCode())
                            player2Id = UUID.fromString(it.bodyAsJsonObject().getString("id"))
                        },
                    client
                        .post("/participants")
                        .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
                        .sendJson(JsonObject(participantData1))
                        .map {
                            assertEquals(201, it.statusCode())
                            participant1Id = UUID.fromString(it.bodyAsJsonObject().getString("id"))
                        },
                ).coAwait()
        }

    @Test
    @Order(2)
    fun `create new games`() =
        runTest {
            Future
                .all(
                    client
                        .post("/gamedata")
                        .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
                        .sendJson(
                            JsonObject(gameData1)
                                .put(
                                    "participants",
                                    listOf(
                                        JsonObject().put("participant_id", player1Id).put("role", "PLAYER"),
                                        JsonObject().put("participant_id", player2Id).put("role", "PLAYER"),
                                        JsonObject().put("participant_id", participant1Id).put("role", "COUCH"),
                                    ),
                                ),
                        ).map { res ->
                            val resJson = res.bodyAsJsonObject()
                            assertEquals(201, res.statusCode())
                            val original =
                                JsonObject(gameData1)
                                    .put("id", resJson.getString("id"))
                                    .put(
                                        "participants",
                                        listOf(
                                            JsonObject().put("participant_id", player1Id.toString()).put("role", "PLAYER"),
                                            JsonObject().put("participant_id", player2Id.toString()).put("role", "PLAYER"),
                                            JsonObject().put("participant_id", participant1Id.toString()).put("role", "COUCH"),
                                        ),
                                    )
                            assertEquals(original, resJson)
                            game1Id = UUID.fromString(res.bodyAsJsonObject().getString("id"))
                        },
                    client
                        .post("/gamedata")
                        .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
                        .sendJson(
                            JsonObject(gameData2).put(
                                "participants",
                                listOf(JsonObject().put("participant_id", player2Id).put("role", "PLAYER")),
                            ),
                        ).map {
                            game2Id = UUID.fromString(it.bodyAsJsonObject().getString("id"))
                        },
                ).coAwait()
        }

    @Test
    @Order(3)
    fun `change a game`() =
        runTest {
            client
                .patch("/gamedata/$game1Id")
                .putHeader("Origin", "http://api.localhost")
                .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
                .sendJson(
                    JsonObject(gameData1)
                        .put("game", "Pacman")
                        .put(
                            "participants",
                            listOf(JsonObject().put("participant_id", player1Id.toString()).put("role", "PLAYER")),
                        ),
                ).map {
                    assertEquals(200, it.statusCode())
                    val expectedData =
                        JsonObject(gameData1)
                            .put("id", game1Id.toString())
                            .put("game", "Pacman")
                            .put(
                                "participants",
                                listOf(JsonObject().put("participant_id", player1Id.toString()).put("role", "PLAYER")),
                            )
                    assertEquals(expectedData, it.bodyAsJsonObject())
                }.coAwait()
        }

    @Test
    @Order(4)
    fun `remove a game`() =
        runTest {
            client
                .delete("/gamedata/$game1Id")
                .putHeader("Origin", "http://api.localhost")
                .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
                .send()
                .map {
                    assertEquals(204, it.statusCode())
                }.compose {
                    client
                        .get("/gamedata/$game1Id")
                        .send()
                }.map {
                    assertEquals(404, it.statusCode())
                }.compose {
                    client
                        .get("/participants/$player1Id")
                        .send()
                }.map {
                    assertEquals(200, it.statusCode())
                }.coAwait()
        }

    @Test
    @Order(5)
    fun `remove a player`() =
        runTest {
            client
                .delete("/participants/$player2Id")
                .putHeader("Origin", "http://api.localhost")
                .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
                .send()
                .map {
                    assertEquals(204, it.statusCode())
                }.compose {
                    client
                        .get("/participants/$player2Id")
                        .send()
                }.map {
                    assertEquals(404, it.statusCode())
                }.compose {
                    client
                        .get("/gamedata/$game2Id")
                        .send()
                }.map {
                    assertEquals(200, it.statusCode())
                    val expectedResponse =
                        JsonObject(gameData2)
                            .put("id", game2Id.toString())
                            .put("participants", listOf<String>())
                    assertEquals(expectedResponse, it.bodyAsJsonObject())
                }.coAwait()
        }

    @Test
    @Order(6)
    fun `modify player info`() =
        runTest {
            client
                .patch("/participants/$player1Id")
                .putHeader("Origin", "http://api.localhost")
                .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
                .sendJson(
                    JsonObject(playerData1)
                        .put("display_name", "Pekka"),
                ).map {
                    assertEquals(200, it.statusCode())
                    val expectedResponse =
                        JsonObject(playerData1)
                            .put("id", player1Id.toString())
                            .put("display_name", "Pekka")
                    assertEquals(expectedResponse, it.bodyAsJsonObject())
                }.compose {
                    client
                        .get("/participants/$player1Id")
                        .send()
                }.map {
                    assertEquals(200, it.statusCode())
                    val expectedResponse =
                        JsonObject(playerData1)
                            .put("id", player1Id.toString())
                            .put("display_name", "Pekka")
                    assertEquals(expectedResponse, it.bodyAsJsonObject())
                }.coAwait()
        }
}
