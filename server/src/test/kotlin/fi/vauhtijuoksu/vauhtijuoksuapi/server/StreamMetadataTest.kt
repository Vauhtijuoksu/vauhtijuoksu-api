package fi.vauhtijuoksu.vauhtijuoksuapi.server

import fi.vauhtijuoksu.vauhtijuoksuapi.models.StreamMetadata
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.lenient
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import java.net.http.HttpResponse
import java.util.UUID

class StreamMetadataTest : ServerTestBase() {
    private val streamMetadataEndpoint = "/stream-metadata"
    private val someUuid = UUID.randomUUID()
    private val someMetadata = StreamMetadata(100, someUuid, listOf("save", "norppas"), listOf(1, 0))
    private val someMetadataJson = JsonObject()
        .put("donation_goal", 100)
        .put("current_game_id", someUuid.toString())
        .put("donatebar_info", listOf("save", "norppas"))
        .put("counters", listOf(1, 0))

    @BeforeEach
    fun before() {
        // Mockito is supposed to be lenient with mocks initialized in @BeforeEach, but for some reason is not
        lenient().`when`(streamMetadataDb.get()).thenReturn(Future.succeededFuture(someMetadata))
        lenient().`when`(streamMetadataDb.save(any())).thenReturn(Future.succeededFuture())
    }

    @AfterEach
    fun after() {
        // We don't care about reads
        verify(streamMetadataDb, atLeast(0)).get()
        verifyNoMoreInteractions(streamMetadataDb)
    }

    private fun patchStreamMetadata(body: JsonObject): Future<io.vertx.ext.web.client.HttpResponse<Buffer>> {
        return client.patch(streamMetadataEndpoint)
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(body)
    }

    @Test
    fun `get returns current stream metadata`(testContext: VertxTestContext) {
        client.get(streamMetadataEndpoint)
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals("application/json", res.getHeader("content-type"))
                    assertEquals(someMetadataJson, res.bodyAsJsonObject())
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `get accepts all origins`(testContext: VertxTestContext) {
        client.get(streamMetadataEndpoint)
            .putHeader("Origin", "https://example.com")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals("*", res.getHeader("Access-Control-Allow-Origin"))
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `patch requires credentials`(testContext: VertxTestContext) {
        client.patch(streamMetadataEndpoint)
            .sendJson("{}")
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(401, res.statusCode())
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `patch accepts vauhtijuoksu origins`(testContext: VertxTestContext) {
        client.patch(streamMetadataEndpoint)
            .putHeader("Origin", "https://vauhtijuoksu.fi")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject())
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals(corsHeaderUrl, res.getHeader("Access-Control-Allow-Origin"))
                    verify(streamMetadataDb).save(someMetadata)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `patch saves updated data and responds with new stream metadata`(testContext: VertxTestContext) {
        patchStreamMetadata(JsonObject().put("donation_goal", 1000))
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals("application/json", res.getHeader("content-type"))
                    someMetadataJson.put("donation_goal", 1000)
                    assertEquals(someMetadataJson, res.bodyAsJsonObject())
                    verify(streamMetadataDb).save(someMetadata.copy(donationGoal = 1000))
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `patch responds bad request on invalid json`(testContext: VertxTestContext) {
        client.patch(streamMetadataEndpoint)
            .authentication(UsernamePasswordCredentials(username, password))
            .sendBuffer(Buffer.buffer().appendString("hello server"))
            .onFailure(testContext::failNow)
            .onSuccess { patchRes ->
                testContext.verify {
                    assertEquals(400, patchRes.statusCode())
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `patch responds bad request when there are unknown fields in input json`(testContext: VertxTestContext) {
        patchStreamMetadata(JsonObject().put("unknown_field", "value"))
            .onFailure(testContext::failNow)
            .onSuccess { patchRes ->
                testContext.verify {
                    assertEquals(400, patchRes.statusCode())
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `patch responds bad request on invalid data format`(testContext: VertxTestContext) {
        patchStreamMetadata(JsonObject().put("counters", listOf("sata")))
            .onFailure(testContext::failNow)
            .onSuccess { patchRes ->
                testContext.verify {
                    assertEquals(400, patchRes.statusCode())
                }
                testContext.completeNow()
            }
    }
}
