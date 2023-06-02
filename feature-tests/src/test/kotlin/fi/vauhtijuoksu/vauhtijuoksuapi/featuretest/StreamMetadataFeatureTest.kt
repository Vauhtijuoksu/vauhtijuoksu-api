package fi.vauhtijuoksu.vauhtijuoksuapi.featuretest

import io.vertx.core.CompositeFuture
import io.vertx.core.json.JsonArray
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

@FeatureTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class StreamMetadataFeatureTest {
    private val newMetaData = """
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
          ]
        }
    """.trimIndent()

    private val metadataResponse = """
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
          "timers": []
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

    private val aTimer = """
        {            
            "start_time": "2021-09-21T15:05:47Z",
            "end_time": "2021-09-21T16:05:47Z",
            "name": "timer 1"
        }
    """.trimIndent()

    private lateinit var client: WebClient

    @BeforeEach
    fun setup(testContext: VertxTestContext, webClient: WebClient) {
        client = webClient
        webClient.get("/timers")
            .send()
            .flatMap { res ->
                CompositeFuture.all(
                    res.bodyAsJsonArray().map { timer ->
                        webClient.delete("/timers/${(timer as JsonObject).getString("id")}")
                            .withAuthenticationAndOrigins()
                            .send()
                    },
                )
            }.map {
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }

    @Test
    @Order(1)
    fun `test changing whole stream metadata`(testContext: VertxTestContext) {
        client.patch("/stream-metadata")
            .withAuthenticationAndOrigins()
            .sendJson(JsonObject(newMetaData))
            .verifyStatusCode(200, testContext)
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals(JsonObject(metadataResponse), res.bodyAsJsonObject())
                }
                testContext.completeNow()
            }
    }

    @Test
    @Order(2)
    fun `test partially updating stream metadata`(testContext: VertxTestContext) {
        client.patch("/stream-metadata")
            .withAuthenticationAndOrigins()
            .sendJson(JsonObject(newMetaData))
            .verifyStatusCode(200, testContext)
            .flatMap {
                client.patch("/stream-metadata")
                    .putHeader("Origin", "http://api.localhost")
                    .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
                    .sendJson(
                        JsonObject()
                            .put("donation_goal", 3000)
                            .put("counters", JsonArray()),
                    )
            }
            .map { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    val expectedData = JsonObject(metadataResponse)
                        .put("donation_goal", 3000)
                        .put("counters", JsonArray())
                    assertEquals(expectedData, res.bodyAsJsonObject())
                }
                testContext.completeNow()
            }
            .onFailure(testContext::failNow)
    }

    @Test
    @Order(3)
    fun `test heart rate update`(testContext: VertxTestContext) {
        client.patch("/stream-metadata")
            .withAuthenticationAndOrigins()
            .sendJson(JsonObject(newMetaData))
            .verifyStatusCode(200, testContext)
            .compose {
                client.patch("/stream-metadata")
                    .withAuthenticationAndOrigins()
                    .sendJson(JsonObject(someHeartData))
            }
            .map { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    val expectedData = JsonObject(metadataResponse)
                        .put("heart_rates", listOf(100, 130, 333, 441))
                    assertEquals(expectedData, res.bodyAsJsonObject())
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }

    @Test
    @Order(4)
    fun `test timer update`(testContext: VertxTestContext) {
        client.patch("/stream-metadata")
            .withAuthenticationAndOrigins()
            .sendJson(JsonObject(newMetaData))
            .verifyStatusCode(200, testContext)
            .flatMap {
                client.post("/timers")
                    .withAuthenticationAndOrigins()
                    .sendJson(
                        JsonObject(aTimer).put("end_time", null),
                    )
            }
            .verifyStatusCode(201, testContext)
            .map {
                val id = it.bodyAsJsonObject().getString("id")
                assertEquals(JsonObject(aTimer).put("end_time", null).put("id", id), it.bodyAsJsonObject())
                id
            }
            .compose { id ->
                client.patch("/timers/$id")
                    .withAuthenticationAndOrigins()
                    .sendJson(JsonObject().put("end_time", JsonObject(aTimer).getString("end_time")))
                    .map(id)
            }
            .flatMap { id ->
                client.get("/stream-metadata").send()
                    .verifyStatusCode(200, testContext)
                    .map {
                        object {
                            val id = id
                            val res = it
                        }
                    }
            }
            .map {
                testContext.verify {
                    assertEquals(200, it.res.statusCode())
                    val expectedData = JsonObject(metadataResponse)
                        .put("timers", JsonArray().add(JsonObject(aTimer).put("id", it.id)))
                    assertEquals(expectedData, it.res.bodyAsJsonObject())
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }
}
