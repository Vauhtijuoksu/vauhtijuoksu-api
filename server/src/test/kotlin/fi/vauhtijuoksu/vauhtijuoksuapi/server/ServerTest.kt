package fi.vauhtijuoksu.vauhtijuoksuapi.server

import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoMoreInteractions

class ServerTest : ServerTestBase() {
    @Test
    fun testServerResponds(testContext: VertxTestContext) {
        client.get("/").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(404, res.statusCode())
                    verifyNoMoreInteractions(gameDataDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testServerRespondsWithOrigin(testContext: VertxTestContext) {
        client.get("/").putHeader("Origin", "https://localhost").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(404, res.statusCode())
                    assertEquals(corsHeader, res.getHeader("Access-Control-Allow-Origin"))
                    verifyNoMoreInteractions(gameDataDb)
                }
                testContext.completeNow()
            }
    }
}
