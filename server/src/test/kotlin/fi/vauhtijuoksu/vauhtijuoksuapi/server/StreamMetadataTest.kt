package fi.vauhtijuoksu.vauhtijuoksuapi.server

import fi.vauhtijuoksu.vauhtijuoksuapi.MockitoUtils.Companion.any
import fi.vauhtijuoksu.vauhtijuoksuapi.models.StreamMetadata
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.streammetadata.StreamMetaDataApiModel
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.timers.TimerApiModel
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
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class StreamMetadataTest : ServerTestBase() {
    private val streamMetadataEndpoint = "/stream-metadata"
    private val someUuid = UUID.randomUUID()
    private val someTimer = Timer(
        UUID.randomUUID(),
        OffsetDateTime.ofInstant(
            Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2022-05-05T16:00:00Z")),
            ZoneId.of("Z"),
        ),
        OffsetDateTime.ofInstant(
            Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2022-05-06T16:00:00Z")),
            ZoneId.of("Z"),
        ),
        "timer 1",
    )
    private val someApiTimer = TimerApiModel(
        someTimer.id,
        someTimer.startTime,
        someTimer.endTime,
        someTimer.name,
    )

    private val someMetadata = StreamMetadata(
        100,
        someUuid,
        listOf("save", "norppas"),
        listOf(1, 0),
        listOf(100, 120),
        "Deerboy - Biisi",
    )

    private val someMetadataNoTimer = StreamMetadata(
        100,
        someUuid,
        listOf("save", "norppas"),
        listOf(1, 0),
        listOf(100, 120),
        "Deerboy - Biisi",
    )

    private val someMetadataApi = StreamMetaDataApiModel(
        100,
        someUuid,
        listOf("save", "norppas"),
        listOf(1, 0),
        listOf(100, 120),
        listOf(someApiTimer),
        "Deerboy - Biisi",
    )

    @BeforeEach
    fun before() {
        // Mockito is supposed to be lenient with mocks initialized in @BeforeEach, but for some reason is not
        lenient().`when`(streamMetadataDatabase.get()).thenReturn(Future.succeededFuture(someMetadata))
        lenient().`when`(streamMetadataDatabase.save(any())).thenReturn(Future.succeededFuture())
        lenient().`when`(timerDb.getAll()).thenReturn(Future.succeededFuture(listOf(someTimer)))
    }

    @AfterEach
    fun after() {
        // We don't care about reads
        verify(streamMetadataDatabase, atLeast(0)).get()
        verifyNoMoreInteractions(streamMetadataDatabase)
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
                    assertEquals(someMetadataApi, res.bodyAsJson(StreamMetaDataApiModel::class.java))
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
                    verify(streamMetadataDatabase).save(someMetadataNoTimer)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `patch saves updated data and responds with new stream metadata`(testContext: VertxTestContext) {
        lenient().`when`(streamMetadataDatabase.get())
            .thenReturn(Future.succeededFuture(someMetadataNoTimer.copy(donationGoal = 1000)))
        `when`(timerDb.getAll()).thenReturn(Future.succeededFuture(listOf()))
        patchStreamMetadata(JsonObject().put("donation_goal", 1000))
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals("application/json", res.getHeader("content-type"))
                    val original = StreamMetaDataApiModel.from(someMetadataNoTimer.copy(donationGoal = 1000), listOf())
                    val response = res.bodyAsJson(StreamMetaDataApiModel::class.java)
                    assertEquals(original, response)
                    verify(streamMetadataDatabase).save(someMetadataNoTimer.copy(donationGoal = 1000))
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
