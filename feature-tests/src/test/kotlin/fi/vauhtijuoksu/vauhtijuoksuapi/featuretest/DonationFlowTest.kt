package fi.vauhtijuoksu.vauhtijuoksuapi.featuretest

import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.ext.web.client.HttpRequest
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.test.runTest
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

    private fun <T> authenticatedRequest(request: HttpRequest<T>): HttpRequest<T> {
        return request
            .putHeader("Origin", "http://api.localhost")
            .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
    }

    private fun authenticatedPost(url: String): HttpRequest<Buffer> {
        return authenticatedRequest(client.post(url))
    }

    private fun authenticatedPatch(url: String): HttpRequest<Buffer> {
        return authenticatedRequest(client.patch(url))
    }

    @Test
    @Order(1)
    fun `an admin can create incentives`() = runTest {
        val postResults = Future.all(
            authenticatedPost("/incentives/")
                .sendJson(JsonObject(newIncentive1)),
            authenticatedPost("/incentives/")
                .sendJson(JsonObject(newIncentive2)),
        ).coAwait()

        val (first, second) = postResults.list<HttpResponse<Buffer>>()
            .map { res ->
                assertEquals(201, res.statusCode())
                res.bodyAsJsonObject().getString("id")
            }
        incentiveId1 = first
        incentiveId2 = second
    }

    @Test
    @Order(2)
    fun `users can generate incentive codes for incentives`() = runTest {
        val postResults = Future.all(
            client.post("/generate-incentive-code")
                .putHeader("Origin", "http://api.localhost")
                .sendJson(JsonArray().add(JsonObject().put("id", incentiveId1).put("parameter", "neliö"))),
            client.post("/generate-incentive-code")
                .putHeader("Origin", "http://api.localhost")
                .sendJson(JsonArray().add(JsonObject().put("id", incentiveId2).put("parameter", "kirkas"))),
        ).coAwait()
        val (first, second) = postResults.list<HttpResponse<Buffer>>().map {
            assertEquals(201, it.statusCode())
            val body = it.bodyAsJsonObject()
            assertEquals(1, body.size())
            body.getString("code")
        }
        incentiveCode1 = first
        incentiveCode2 = second
    }

    @Test
    @Order(3)
    fun `then one user makes a donation using the wrong generated code`() = runTest {
        val donation = authenticatedPost("/donations")
            .sendJson(JsonObject(newDonation).put("message", "ota tää :D 🤔🤔 $incentiveCode2"))
            .coAwait()
        donationId = donation.bodyAsJsonObject().getString("id")
    }

    @Test
    @Order(4)
    fun `the user checks incentives, but doesn't see their money because it's on the wrong incentive`() = runTest {
        client.get("/incentives/$incentiveId1")
            .send()
            .coAwait().let {
                val incentive = it.bodyAsJsonObject()
                val total = incentive.getDouble("total_amount")
                assertEquals(0.0, total)
                val statuses = incentive.getJsonArray("status")
                assertEquals(0, statuses.size())
            }

        client.get("/incentives/$incentiveId2")
            .send()
            .coAwait().let {
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
    }

    @Test
    @Order(5)
    fun `the kind admin fixed the message`() = runTest {
        authenticatedPatch("/donations/$donationId")
            .sendJson(JsonObject().put("message", "ota tää :D 🤔🤔: $incentiveCode1"))
            .coAwait().let {
                assertEquals(200, it.statusCode())
            }
    }

    @Test
    @Order(6)
    fun `the user checks incentives, and sees their donation money in the status and gone from the wrong incentive`() = runTest {
        client.get("/incentives/$incentiveId1")
            .send()
            .coAwait().let {
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
        client.get("/incentives/$incentiveId2")
            .send()
            .coAwait()
            .let {
                val incentive = it.bodyAsJsonObject()
                val total = incentive.getDouble("total_amount")
                assertEquals(0.0, total)
                val statuses = incentive.getJsonArray("status")
                assertEquals(0, statuses.size())
            }
    }

    @Test
    @Order(7)
    fun `the donation shows up in donations list`() = runTest {
        client.get("/donations")
            .send()
            .coAwait()
            .let {
                val donations = it.bodyAsJsonArray()
                val donation = donations.find {
                    (it as JsonObject).getString("id") == donationId
                } as JsonObject
                assertEquals(donationId, donation.getString("id"))
                assertEquals(donationSum, donation.getDouble("amount"))
                assertEquals("ota tää :D 🤔🤔: $incentiveCode1", donation.getString("message"))
            }
    }
}
