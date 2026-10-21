package fi.vauhtijuoksu.vauhtijuoksuapi.featuretest

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@FeatureTest
class CharityFeatureTest {
    private lateinit var client: WebClient

    @BeforeEach
    fun setup(webClient: WebClient) {
        client = webClient
    }

    @Test
    fun `a charity can be added, read, changed and deleted`() = runTest {
        val created = client.post("/charities")
            .withAuthenticationAndOrigins()
            .sendJson(JsonObject().put("name", "Kissojen kerho").put("img_filename", "kissa.png"))
            .coAwait()
        assertEquals(201, created.statusCode())
        val id = created.bodyAsJsonObject().getString("id")

        val all = client.get("/charities").send().coAwait()
        assertTrue(all.bodyAsJsonArray().any { (it as JsonObject).getString("id") == id })

        val single = client.get("/charities/$id").send().coAwait()
        assertEquals("Kissojen kerho", single.bodyAsJsonObject().getString("name"))

        val patched = client.patch("/charities/$id")
            .withAuthenticationAndOrigins()
            .sendJson(JsonObject().put("name", "Kissojen klubi").put("goal", 500))
            .coAwait()
        assertEquals(200, patched.statusCode())
        assertEquals("Kissojen klubi", patched.bodyAsJsonObject().getString("name"))
        assertEquals(500, patched.bodyAsJsonObject().getInteger("goal"))

        val donationWithoutCharity = client.post("/donations")
            .withAuthenticationAndOrigins()
            .sendJson(JsonObject().put("timestamp", "2026-10-19T12:00:00Z").put("name", "testi").put("amount", 1))
            .coAwait()
        assertEquals(400, donationWithoutCharity.statusCode())

        val deleted = client.delete("/charities/$id")
            .withAuthenticationAndOrigins()
            .send()
            .coAwait()
        assertEquals(204, deleted.statusCode())
        assertEquals(404, client.get("/charities/$id").send().coAwait().statusCode())
    }
}
