package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.google.inject.AbstractModule
import com.google.inject.Guice
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.ServerConfiguration
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
import java.net.ServerSocket

@ExtendWith(VertxExtension::class)
class ServerTest {
    private lateinit var vertx: Vertx
    private lateinit var server: Server
    private lateinit var client: WebClient

    private fun getFreePort(): Int {
        val sock = ServerSocket(0)
        val port = sock.localPort
        sock.close()
        return port
    }

    @BeforeEach
    fun beforeEach() {
        val serverPort = getFreePort()
        val injector = Guice.createInjector(
            ApiModule(),
            object : AbstractModule() {
                override fun configure() {
                    bind(ServerConfiguration::class.java).toInstance(ServerConfiguration(serverPort))
                }
            }
        )

        vertx = injector.getInstance(Vertx::class.java)
        server = injector.getInstance(Server::class.java)
        server.start()
        client = WebClient.create(vertx, WebClientOptions().setDefaultPort(serverPort))
    }

    @AfterEach
    fun afterEach() {
        server.stop()
        client.close()
    }

    @Test
    fun testServerResponds(testContext: VertxTestContext) {
        client.get("/").send().onComplete { res ->
            testContext.verify {
                assertTrue(res.succeeded())
                assertEquals(200, res.result().statusCode())
                assertEquals("Hello world", res.result().bodyAsString())
            }
            testContext.completeNow()
        }
    }
}
