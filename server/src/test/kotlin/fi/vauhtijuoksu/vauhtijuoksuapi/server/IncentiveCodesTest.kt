package fi.vauhtijuoksu.vauhtijuoksuapi.server

import fi.vauhtijuoksu.vauhtijuoksuapi.MockitoUtils.Companion.any
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestIncentive
import io.vertx.core.Future
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.json.get
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`

class IncentiveCodesTest : ServerTestBase() {
    private val milestoneIncentive = TestIncentive.incentive1

    @Test
    fun `unauthenticated users can generate incentive codes`(testContext: VertxTestContext) {
        `when`(incentiveDatabase.getById(milestoneIncentive.id))
            .thenReturn(Future.succeededFuture(milestoneIncentive))
        `when`(generatedIncentiveCodeDatabase.add(any())).thenReturn(Future.succeededFuture())
        client.post("/generate-incentive-code")
            .putHeader("Origin", "https://example.com")
            .sendJson(JsonArray().add(JsonObject().put("id", milestoneIncentive.id)))
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(201, res.statusCode())
                    assertNotNull(res.bodyAsJsonObject()["code"])
                }
                testContext.completeNow()
            }
    }
}
