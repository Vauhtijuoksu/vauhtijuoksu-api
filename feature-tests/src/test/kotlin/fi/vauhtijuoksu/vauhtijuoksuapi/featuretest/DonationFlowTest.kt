package fi.vauhtijuoksu.vauhtijuoksuapi.featuretest

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.ext.web.client.HttpRequest
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ExtendWith(VertxExtension::class)
class DonationFlowTest {
    private val newIncentive = """{
      "game_id": null,
      "title": "Tetrispalikan nimi",
      "end_time": "2021-09-21T19:08:47.000+00:00",
      "type": "open",
      "open_char_limit": 10,
      "info": "Nimeä tetrispalikka poggers"    
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
        private lateinit var incentiveId: UUID
        private lateinit var incentiveCode: String
    }

    @BeforeEach
    fun setup() {
        client = WebClient.create(Vertx.vertx())
    }

    private fun authenticatedPost(url: String): HttpRequest<Buffer> {
        return client.post(url)
            .putHeader("Origin", "http://localhost")
            .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
    }

    @Test
    @Order(1)
    fun `an admin can create an incentive`(testContext: VertxTestContext) {
        authenticatedPost("/incentives/")
            .sendJson(JsonObject(newIncentive))
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(201, res.statusCode())
                }
                incentiveId = UUID.fromString(res.bodyAsJsonObject().getString("id"))
                testContext.completeNow()
            }
    }

    @Test
    @Order(2)
    fun `a user can generate an incentive code for the incentive`(testContext: VertxTestContext) {
        client.post("/generate-incentive-code")
            .putHeader("Origin", "http://localhost")
            .sendJson(JsonArray().add(JsonObject().put("id", incentiveId.toString()).put("parameter", "neliö")))
            .onFailure(testContext::failNow)
            .onSuccess {
                testContext.verify {
                    assertEquals(201, it.statusCode())
                    val body = it.bodyAsJsonObject()
                    assertEquals(1, body.size())
                    incentiveCode = body.getString("code")
                }
                testContext.completeNow()
            }
    }

    @Test
    @Order(3)
    fun `then the user makes a donation using the generated code`(testContext: VertxTestContext) {
        authenticatedPost("/donations")
            .sendJson(JsonObject(newDonation).put("message", "ota tää :D 🤔🤔: $incentiveCode"))
            .onFailure(testContext::failNow)
            .onSuccess {
                testContext.completeNow()
            }
    }

    @Test
    @Order(4)
    fun `the user checks incentives, and sees their donation money in the status`(testContext: VertxTestContext) {
        client.get("/incentives/$incentiveId")
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
                testContext.completeNow()
            }
    }
}
