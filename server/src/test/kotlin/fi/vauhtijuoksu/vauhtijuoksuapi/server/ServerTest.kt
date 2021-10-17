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
                    verifyNoMoreInteractions(db)
                }
                testContext.completeNow()
            }
    }
}
