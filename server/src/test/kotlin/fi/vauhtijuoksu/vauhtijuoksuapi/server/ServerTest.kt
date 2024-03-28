package fi.vauhtijuoksu.vauhtijuoksuapi.server

import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ServerTest : ServerTestBase() {
    @Test
    fun testServerRespondsWithOptions() = runTest {
        client.request(HttpMethod.OPTIONS, "/")
            .putHeader(HttpHeaders.ORIGIN.toString(), "https://newapi.vauhtijuoksu.fi")
            .send()
            .map { res ->
                val allowedMethods = setOf("GET", "POST", "PATCH", "OPTIONS", "DELETE")
                assertEquals(allowedMethods, res.headers().get("Allow").split(", ").toSet())
                assertEquals("https://newapi.vauhtijuoksu.fi", res.getHeader("Access-Control-Allow-Origin"))
            }.coAwait()
    }
}
