package fi.vauhtijuoksu.vauhtijuoksuapi.server

import fi.vauhtijuoksu.vauhtijuoksuapi.models.PlayerInfo
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
    fun `get returns current player info`(testContext: VertxTestContext) {
        client.get(playerInfoEndpoint)
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals("application/json", res.getHeader("content-type"))
                    assertEquals(somePlayerInfoJson, res.bodyAsJsonObject())
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `get accepts all origins`(testContext: VertxTestContext) {
        client.get(playerInfoEndpoint)
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
        client.patch(playerInfoEndpoint)
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
        client.patch(playerInfoEndpoint)
            .putHeader("Origin", "https://vauhtijuoksu.fi")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject())
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals(corsHeaderUrl, res.getHeader("Access-Control-Allow-Origin"))
                    verify(playerInfoDb).save(somePlayerInfo)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `patch saves updated data and responds with new player info`(testContext: VertxTestContext) {
        patchPlayerInfo(JsonObject().put("message", "Hello world"))
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals("application/json", res.getHeader("content-type"))
                    somePlayerInfoJson.put("message", "Hello world")
                    assertEquals(somePlayerInfoJson, res.bodyAsJsonObject())
                    verify(playerInfoDb).save(somePlayerInfo.copy(message = "Hello world"))
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `patch responds bad request on invalid json`(testContext: VertxTestContext) {
        client.patch(playerInfoEndpoint)
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
        patchPlayerInfo(JsonObject().put("unknown_field", "value"))
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
        patchPlayerInfo(JsonObject().put("counters", listOf("sata")))
            .onFailure(testContext::failNow)
            .onSuccess { patchRes ->
                testContext.verify {
                    assertEquals(400, patchRes.statusCode())
                }
                testContext.completeNow()
            }
    }
}
