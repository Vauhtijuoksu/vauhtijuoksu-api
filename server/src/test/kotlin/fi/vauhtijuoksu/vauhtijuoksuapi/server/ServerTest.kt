package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.google.inject.Guice
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class ServerTest {
    private lateinit var vertx: Vertx
    private lateinit var server: Server
    private lateinit var client: WebClient

    @BeforeEach
    fun beforeEach() {
        val injector = Guice.createInjector(ApiModule())
        vertx = injector.getInstance(Vertx::class.java)
        server = injector.getInstance(Server::class.java)
        server.start()
        client = WebClient.create(vertx, WebClientOptions().setDefaultPort(8080))
    }

    @AfterEach
    fun afterEach(testContext: VertxTestContext) {
        server.stop()
        client.close()
        vertx.close { testContext.completeNow() }
    }

    @Test
    fun testServerResponds(testContext: VertxTestContext) {
        client.get("http://localhost:8080").send().onComplete { res ->
            testContext.verify {
                assertTrue(res.succeeded())
                assertEquals(200, res.result().statusCode())
                assertEquals("Hello world", res.result().bodyAsString())
            }
            testContext.completeNow()
        }
    }
}
