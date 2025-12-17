package fi.vauhtijuoksu.vauhtijuoksuapi.featuretest

import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.ext.web.client.HttpRequest
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.time.OffsetDateTime
import java.util.UUID

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@FeatureTest
class IncentiveTest {
    private val newIncentive =
        """
        {
          "game_id": null,
          "title": "Tetrispakikan nimi",
          "end_time": "2021-09-21T19:08:47.000+00:00",
          "type": "open",
          "open_char_limit": 10,
          "info": "Nimeä tetrispalikka poggers"    
        }
        """.trimIndent()

    private lateinit var client: WebClient
    private val incentives = "/incentives/"

    companion object {
        private lateinit var addedId: UUID
    }

    @BeforeEach
    fun setup(webClient: WebClient) {
        client = webClient
    }

    private fun authenticatedRequest(
        method: HttpMethod,
        url: String,
    ): HttpRequest<Buffer> =
        client
            .request(method, url)
            .putHeader("Origin", "http://api.localhost")
            .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))

    private fun getAll(): HttpRequest<Buffer> =
        client
            .get("$incentives/")
            .putHeader("Origin", "http://api.localhost")

    private fun get(id: UUID): HttpRequest<Buffer> =
        client
            .get("$incentives/$id")
            .putHeader("Origin", "http://api.localhost")

    @Test
    @Order(1)
    fun `test adding incentive`(testContext: VertxTestContext) {
        authenticatedRequest(HttpMethod.POST, incentives)
            .sendJson(JsonObject(newIncentive))
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(201, res.statusCode())
                    val body = res.bodyAsJsonObject()
                    addedId = UUID.fromString(body.remove("id") as String?)
                    val expectedBody = JsonObject(newIncentive)
                    expectedBody.put("total_amount", 0.0)
                    expectedBody.put("status", JsonArray())

                    val expectedEndTime = OffsetDateTime.parse(expectedBody.remove("end_time") as String?)
                    val receivedEndTime = OffsetDateTime.parse(body.remove("end_time") as String?)

                    assertEquals(expectedBody, body)
                    assertTrue(expectedEndTime.isEqual(receivedEndTime))
                }
                testContext.completeNow()
            }
    }

    @Test
    @Order(2)
    fun `test getting all incentives`(testContext: VertxTestContext) {
        getAll()
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    val body = res.bodyAsJsonArray()
                    assertTrue(body.size() >= 1)
                }
                testContext.completeNow()
            }
    }

    @Test
    @Order(2)
    fun `test single incentive`(testContext: VertxTestContext) {
        get(addedId)
            .send()
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    val body = res.bodyAsJsonObject()
                    val expected = JsonObject(newIncentive)
                    expected.put("id", addedId.toString())
                    expected.put("total_amount", 0.0)
                    expected.put("status", JsonArray())

                    val expectedEndTime = OffsetDateTime.parse(expected.remove("end_time") as String?)
                    val receivedEndTime = OffsetDateTime.parse(body.remove("end_time") as String?)

                    assertEquals(expected, body)
                    assertTrue(expectedEndTime.isEqual(receivedEndTime))
                }
                testContext.completeNow()
            }
    }

    @Test
    @Order(3)
    fun `test changing info`(testContext: VertxTestContext) {
        authenticatedRequest(HttpMethod.PATCH, "$incentives/$addedId")
            .sendJson(JsonObject().put("title", "new title"))
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    val expected =
                        JsonObject(newIncentive)
                            .put("id", addedId.toString())
                            .put("title", "new title")
                            .put("total_amount", 0.0)
                            .put("status", JsonArray())

                    val body = res.bodyAsJsonObject()

                    val expectedEndTime = OffsetDateTime.parse(expected.remove("end_time") as String?)
                    val receivedEndTime = OffsetDateTime.parse(body.remove("end_time") as String?)

                    assertEquals(expected, body)
                    assertTrue(expectedEndTime.isEqual(receivedEndTime))
                }
                testContext.completeNow()
            }
    }

    @Test
    @Order(4)
    fun `test deleting incentive`(testContext: VertxTestContext) {
        authenticatedRequest(HttpMethod.DELETE, "$incentives/$addedId")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(204, res.statusCode())
                }
                testContext.completeNow()
            }
    }
}
