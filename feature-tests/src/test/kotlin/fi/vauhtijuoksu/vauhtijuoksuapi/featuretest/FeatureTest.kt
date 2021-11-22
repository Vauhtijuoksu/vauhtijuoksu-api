package fi.vauhtijuoksu.vauhtijuoksuapi.featuretest

import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class FeatureTest {
    @Test
    fun testServerResponds(testContext: VertxTestContext) {
        val client = WebClient.create(Vertx.vertx())
        client.get("/gamedata")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                }
                testContext.completeNow()
            }
    }
}
