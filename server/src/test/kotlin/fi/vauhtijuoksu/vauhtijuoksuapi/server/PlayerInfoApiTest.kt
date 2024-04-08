package fi.vauhtijuoksu.vauhtijuoksuapi.server

import fi.vauhtijuoksu.vauhtijuoksuapi.MockitoUtils.Companion.any
import fi.vauhtijuoksu.vauhtijuoksuapi.models.PlayerInfo
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.lenient
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

class PlayerInfoApiTest : ServerTestBase() {
    private val playerInfoEndpoint = "/player-info"
    private val somePlayerInfo = PlayerInfo("This is a test")
    private val somePlayerInfoJson = JsonObject()
        .put("message", "This is a test")

    @BeforeEach
    fun before() {
        // Mockito is supposed to be lenient with mocks initialized in @BeforeEach, but for some reason is not
        lenient().`when`(playerInfoDb.get()).thenReturn(Future.succeededFuture(somePlayerInfo))
        lenient().`when`(playerInfoDb.save(any())).thenReturn(Future.succeededFuture())
    }

    @AfterEach
    fun after() {
        // We don't care about reads
        verify(playerInfoDb, atLeast(0)).get()
        verifyNoMoreInteractions(playerInfoDb)
    }

    private fun patchPlayerInfo(body: JsonObject): Future<io.vertx.ext.web.client.HttpResponse<Buffer>> {
        return client.patch(playerInfoEndpoint)
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(body)
    }

    @Test
    fun `get returns current player info`() = runTest {
        client.get(playerInfoEndpoint)
            .send()
            .coAwait()
            .let { res ->
                assertEquals(200, res.statusCode())
                assertEquals("application/json", res.getHeader("content-type"))
                assertEquals(somePlayerInfoJson, res.bodyAsJsonObject())
            }
    }

    @Test
    fun `get accepts all origins`() = runTest {
        client.get(playerInfoEndpoint)
            .putHeader("Origin", "https://example.com")
            .send()
            .coAwait()
            .let { res ->
                assertEquals("*", res.getHeader("Access-Control-Allow-Origin"))
            }
    }

    @Test
    fun `patch requires credentials`() = runTest {
        client.patch(playerInfoEndpoint)
            .sendJson("{}")
            .coAwait()
            .let { res ->
                assertEquals(401, res.statusCode())
            }
    }

    @Test
    fun `patch accepts vauhtijuoksu origins`() = runTest {
        client.patch(playerInfoEndpoint)
            .putHeader("Origin", "https://vauhtijuoksu.fi")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject())
            .coAwait()
            .let { res ->
                assertEquals(200, res.statusCode())
                assertEquals(corsHeaderUrl, res.getHeader("Access-Control-Allow-Origin"))
                verify(playerInfoDb).save(somePlayerInfo)
            }
    }

    @Test
    fun `patch saves updated data and responds with new player info`() = runTest {
        patchPlayerInfo(JsonObject().put("message", "Hello world"))
            .coAwait()
            .let { res ->
                assertEquals(200, res.statusCode())
                assertEquals("application/json", res.getHeader("content-type"))
                somePlayerInfoJson.put("message", "Hello world")
                assertEquals(somePlayerInfoJson, res.bodyAsJsonObject())
                verify(playerInfoDb).save(somePlayerInfo.copy(message = "Hello world"))
            }
    }

    @Test
    fun `patch responds bad request on invalid json`() = runTest {
        client.patch(playerInfoEndpoint)
            .authentication(UsernamePasswordCredentials(username, password))
            .sendBuffer(Buffer.buffer().appendString("hello server"))
            .coAwait()
            .let { patchRes ->
                assertEquals(400, patchRes.statusCode())
            }
    }

    @Test
    fun `patch responds bad request when there are unknown fields in input json`() = runTest {
        patchPlayerInfo(JsonObject().put("unknown_field", "value"))
            .coAwait()
            .let { patchRes ->
                assertEquals(400, patchRes.statusCode())
            }
    }

    @Test
    fun `patch responds bad request on invalid data format`() = runTest {
        patchPlayerInfo(JsonObject().put("counters", listOf("sata")))
            .coAwait()
            .let { patchRes ->
                assertEquals(400, patchRes.statusCode())
            }
    }
}
