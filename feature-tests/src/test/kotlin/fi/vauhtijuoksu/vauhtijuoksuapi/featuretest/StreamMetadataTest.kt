package fi.vauhtijuoksu.vauhtijuoksuapi.featuretest

import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
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
class StreamMetadataTest {
    private val someMetadata = """
        {
          "donation_goal": 10000,
          "current_game_id": null,
          "donatebar_info": [
            "lahjota rahhaa",
            "mee töihi :D"
          ],
          "counters": [
            1,
            7,
            6,
            9
          ]
        }
    """.trimIndent()
    lateinit var client: WebClient

    @BeforeEach
    fun setup() {
        client = WebClient.create(Vertx.vertx())
    }

    @Test
    fun `test changing whole stream metadata`(testContext: VertxTestContext) {
        client.patch("/stream-metadata")
            .putHeader("Origin", "http://localhost")
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
            .putHeader("Origin", "http://localhost")
            .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
            .sendJson(JsonObject(someMetadata))
            .compose {
                client.patch("/stream-metadata")
                    .putHeader("Origin", "http://localhost")
                    .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
                    .sendJson(
                        JsonObject()
                            .put("donation_goal", 3000)
                            .put("counters", JsonArray())
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
}
