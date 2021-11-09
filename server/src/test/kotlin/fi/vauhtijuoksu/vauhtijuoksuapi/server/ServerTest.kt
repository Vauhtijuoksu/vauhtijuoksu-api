package fi.vauhtijuoksu.vauhtijuoksuapi.server

import io.vertx.core.http.HttpMethod
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ServerTest : ServerTestBase() {
    @Test
    fun testServerRespondsWithOptions(testContext: VertxTestContext) {
        client.request(HttpMethod.OPTIONS, "/")
            .putHeader("Origin", "http://example.com")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    val allowedMethods = setOf("GET", "POST", "PATCH", "OPTIONS", "DELETE")
                    assertEquals(allowedMethods, res.headers().get("Allow").split(", ").toSet())
                    assertEquals("*", res.getHeader("Access-Control-Allow-Origin"))
                }
                testContext.completeNow()
            }
    }
}
