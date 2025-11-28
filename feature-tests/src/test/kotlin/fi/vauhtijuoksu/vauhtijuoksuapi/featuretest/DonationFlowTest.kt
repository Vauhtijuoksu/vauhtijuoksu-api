package fi.vauhtijuoksu.vauhtijuoksuapi.featuretest

import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.ext.web.client.HttpRequest
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@FeatureTest
class DonationFlowTest {
    private val newIncentive1 = """{
      "game_id": null,
      "title": "Tetrispalikan nimi",
      "end_time": "2021-09-21T19:08:47.000+00:00",
      "type": "open",
      "open_char_limit": 10,
      "info": "Nimeä tetrispalikka poggers"    
    }
    """

    private val newIncentive2 = """{
      "game_id": null,
      "title": "Tetrispalikan nimi",
      "end_time": "2021-09-21T19:08:47.000+00:00",
      "type": "open",
      "open_char_limit": 10,
      "info": "Ehdota tetrispalikan väriä"    
    }
    """

    private val donationSum = 10.0
    private val newDonation = """ {
      "timestamp": "2021-09-21T15:05:47.000+00:00",
      "name": "jsloth",
      "read": false,
      "amount": $donationSum,
      "external_id": "a non unique string"
    }
    """

    private lateinit var client: WebClient

    companion object {
        private lateinit var incentiveId1: String
        private lateinit var incentiveId2: String
        private lateinit var incentiveCode1: String
        private lateinit var incentiveCode2: String
        private lateinit var donationId: String
    }

    @BeforeEach
    fun setup(webClient: WebClient) {
        client = webClient
    }

    private fun <T> authenticatedRequest(request: HttpRequest<T>): HttpRequest<T> =
        request
            .putHeader("Origin", "http://api.localhost")
            .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))

    private fun authenticatedPost(url: String): HttpRequest<Buffer> = authenticatedRequest(client.post(url))

    private fun authenticatedPatch(url: String): HttpRequest<Buffer> = authenticatedRequest(client.patch(url))

    @Test
    @Order(1)
    fun `an admin can create incentives`(testContext: VertxTestContext) {
        Future
            .all(
                authenticatedPost("/incentives/")
                    .sendJson(JsonObject(newIncentive1)),
                authenticatedPost("/incentives/")
                    .sendJson(JsonObject(newIncentive2)),
            ).onFailure(testContext::failNow)
            .onSuccess {
                val (first, second) =
                    it
                        .list<HttpResponse<Buffer>>()
                        .map { res ->
                            testContext.verify {
                                assertEquals(201, res.statusCode())
                            }
                            res.bodyAsJsonObject().getString("id")
                        }
                incentiveId1 = first
                incentiveId2 = second
                testContext.completeNow()
            }
    }

    @Test
    @Order(2)
    fun `users can generate incentive codes for incentives`(testContext: VertxTestContext) {
        Future
            .all(
                client
                    .post("/generate-incentive-code")
                    .putHeader("Origin", "http://api.localhost")
                    .sendJson(JsonArray().add(JsonObject().put("id", incentiveId1).put("parameter", "neliö"))),
                client
                    .post("/generate-incentive-code")
                    .putHeader("Origin", "http://api.localhost")
                    .sendJson(JsonArray().add(JsonObject().put("id", incentiveId2).put("parameter", "kirkas"))),
            ).onFailure(testContext::failNow)
            .onSuccess {
                testContext.verify {
                    val (first, second) =
                        it.list<HttpResponse<Buffer>>().map {
                            assertEquals(201, it.statusCode())
                            val body = it.bodyAsJsonObject()
                            assertEquals(1, body.size())
                            body.getString("code")
                        }
                    incentiveCode1 = first
                    incentiveCode2 = second
                }
                testContext.completeNow()
            }
    }

    @Test
    @Order(3)
    fun `then one user makes a donation using the wrong generated code`(testContext: VertxTestContext) {
        authenticatedPost("/donations")
            .sendJson(JsonObject(newDonation).put("message", "ota tää :D 🤔🤔 $incentiveCode2"))
            .onFailure(testContext::failNow)
            .onSuccess {
                donationId = it.bodyAsJsonObject().getString("id")
                testContext.completeNow()
            }
    }

    @Test
    @Order(4)
    fun `the user checks incentives, but doesn't see their money because it's on the wrong incentive`(testContext: VertxTestContext) {
        val cp = testContext.checkpoint(2)
        client
            .get("/incentives/$incentiveId1")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess {
                testContext.verify {
                    val incentive = it.bodyAsJsonObject()
                    val total = incentive.getDouble("total_amount")
                    assertEquals(0.0, total)
                    val statuses = incentive.getJsonArray("status")
                    assertEquals(0, statuses.size())
                }
                cp.flag()
            }

        client
            .get("/incentives/$incentiveId2")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess {
                testContext.verify {
                    val incentive = it.bodyAsJsonObject()
                    val total = incentive.getDouble("total_amount")
                    assertEquals(donationSum, total)
                    val statuses = incentive.getJsonArray("status")
                    assertEquals(1, statuses.size())
                    val expectedStatus = JsonObject()
                    expectedStatus.put("type", "option")
                    expectedStatus.put("option", "kirkas")
                    expectedStatus.put("amount", donationSum)
                    val status = statuses.getJsonObject(0)
                    assertEquals(expectedStatus, status)
                }
                cp.flag()
            }
    }

    @Test
    @Order(5)
    fun `the kind admin fixed the message`(testContext: VertxTestContext) {
        authenticatedPatch("/donations/$donationId")
            .sendJson(JsonObject().put("message", "ota tää :D 🤔🤔: $incentiveCode1"))
            .onFailure(testContext::failNow)
            .onSuccess {
                testContext.verify {
                    assertEquals(200, it.statusCode())
                }
                testContext.completeNow()
            }
    }

    @Test
    @Order(6)
    fun `the user checks incentives, and sees their donation money in the status and gone from the wrong incentive`(
        testContext: VertxTestContext,
    ) {
        val cp = testContext.checkpoint(2)
        client
            .get("/incentives/$incentiveId1")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess {
                testContext.verify {
                    val incentive = it.bodyAsJsonObject()
                    val total = incentive.getDouble("total_amount")
                    assertEquals(donationSum, total)
                    val statuses = incentive.getJsonArray("status")
                    assertEquals(1, statuses.size())
                    val expectedStatus = JsonObject()
                    expectedStatus.put("type", "option")
                    expectedStatus.put("option", "neliö")
                    expectedStatus.put("amount", donationSum)
                    val status = statuses.getJsonObject(0)
                    assertEquals(expectedStatus, status)
                }
                cp.flag()
            }

        client
            .get("/incentives/$incentiveId2")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess {
                testContext.verify {
                    val incentive = it.bodyAsJsonObject()
                    val total = incentive.getDouble("total_amount")
                    assertEquals(0.0, total)
                    val statuses = incentive.getJsonArray("status")
                    assertEquals(0, statuses.size())
                }
                cp.flag()
            }
    }

    @Test
    @Order(7)
    fun `the donation shows up in donations list`(testContext: VertxTestContext) {
        client
            .get("/donations")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess {
                testContext.verify {
                    val donations = it.bodyAsJsonArray()
                    val donation =
                        donations.find {
                            (it as JsonObject).getString("id") == donationId
                        } as JsonObject
                    assertEquals(donationId, donation.getString("id"))
                    assertEquals(donationSum, donation.getDouble("amount"))
                    assertEquals("ota tää :D 🤔🤔: $incentiveCode1", donation.getString("message"))
                }
                testContext.completeNow()
            }
    }
}
