package fi.vauhtijuoksu.vauhtijuoksuapi.featuretest

import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@FeatureTest
class BasicTest {
    @Test
    fun testServerResponds(
        webClient: WebClient,
        testContext: VertxTestContext,
    ) {
        webClient
            .get("/gamedata")
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
