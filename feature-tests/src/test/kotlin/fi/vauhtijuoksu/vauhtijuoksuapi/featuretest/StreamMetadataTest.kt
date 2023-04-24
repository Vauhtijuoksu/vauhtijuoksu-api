package fi.vauhtijuoksu.vauhtijuoksuapi.featuretest

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@FeatureTest
class StreamMetadataTest {
    private val someMetadata = """
        {
          "donation_goal": 10000,
          "current_game_id": null,
          "now_playing": null,
          "donatebar_info": [
            "lahjota rahhaa",
            "mee töihi :D"
          ],
          "counters": [
            1,
            7,
            6,
            9
          ],
          "heart_rates": [
              1,
              2,
              3,
              4
          ],
          "timers": [
                {            
                    "start_time": "2020-09-21T15:05:47Z",
                    "end_time": "2020-09-21T16:05:47Z",
                    "indexcol": 1
                }
            ]
        }
    """.trimIndent()

    private val someHeartData = """
        {
          "heart_rates": [
              100,
              130,
              333,
              441
          ]
        }
    """.trimIndent()

    private val someTimerData = """
        {
            "timers": [
                {            
                    "start_time": "2021-09-21T15:05:47Z",
                    "end_time": "2021-09-21T16:05:47Z",
                    "indexcol": 1
                }
            ]
        }
    """.trimIndent()
    private lateinit var client: WebClient

    @BeforeEach
    fun setup(webClient: WebClient) {
        client = webClient
    }

    @Test
    fun `test changing whole stream metadata`(testContext: VertxTestContext) {
        client.patch("/stream-metadata")
            .putHeader("Origin", "http://api.localhost")
            .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
            .sendJson(JsonObject(someMetadata))
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals(JsonObject(someMetadata), res.bodyAsJsonObject())
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `test partially updating stream metadata`(testContext: VertxTestContext) {
        client.patch("/stream-metadata")
            .putHeader("Origin", "http://api.localhost")
            .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
            .sendJson(JsonObject(someMetadata))
            .compose {
                client.patch("/stream-metadata")
                    .putHeader("Origin", "http://api.localhost")
                    .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
                    .sendJson(
                        JsonObject()
                            .put("donation_goal", 3000)
                            .put("counters", JsonArray()),
                    )
            }
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    val expectedData = JsonObject(someMetadata)
                        .put("donation_goal", 3000)
                        .put("counters", JsonArray())
                    assertEquals(expectedData, res.bodyAsJsonObject())
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }

    @Test
    fun `test heart rate update`(testContext: VertxTestContext) {
        client.patch("/stream-metadata")
            .putHeader("Origin", "http://api.localhost")
            .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
            .sendJson(JsonObject(someMetadata))
            .compose {
                client.patch("/stream-metadata")
                    .putHeader("Origin", "http://api.localhost")
                    .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
                    .sendJson(JsonObject(someHeartData))
            }
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    val expectedData = JsonObject(someMetadata)
                        .put("heart_rates", listOf(100, 130, 333, 441))
                    assertEquals(expectedData, res.bodyAsJsonObject())
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }

    @Test
    fun `test timer update`(testContext: VertxTestContext) {
        client.patch("/stream-metadata")
            .putHeader("Origin", "http://api.localhost")
            .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
            .sendJson(JsonObject(someMetadata))
            .compose {
                client.patch("/stream-metadata")
                    .putHeader("Origin", "http://api.localhost")
                    .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
                    .sendJson(JsonObject(someTimerData))
            }
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    val expectedData = JsonObject(someMetadata)
                        .put(
                            "timers",
                            listOf(
                                JsonObject()
                                    .put("start_time", "2021-09-21T15:05:47Z")
                                    .put("end_time", "2021-09-21T16:05:47Z")
                                    .put("indexcol", 1),
                            ),
                        )
                    assertEquals(expectedData, res.bodyAsJsonObject())
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }
}
