package fi.vauhtijuoksu.vauhtijuoksuapi.featuretest

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class PlayerInfoTest {
    private val somePlayerInfo = """
        {
          "message": "asenna gentoo"
        }
    """.trimIndent()
    lateinit var client: WebClient

    @BeforeEach
    fun setup() {
        client = WebClient.create(Vertx.vertx())
    }

    @Test
    fun `test changing message`(testContext: VertxTestContext) {
        client.patch("/player-info")
            .putHeader("Origin", "http://localhost")
            .authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
            .sendJson(JsonObject(somePlayerInfo))
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals(JsonObject(somePlayerInfo), res.bodyAsJsonObject())
                }
                testContext.completeNow()
            }
    }
}
