package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.TypeLiteral
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.ServerConfiguration
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.ServerSocket

@ExtendWith(MockitoExtension::class)
@ExtendWith(VertxExtension::class)
abstract class ServerTestBase {
    private lateinit var vertx: Vertx
    private lateinit var server: Server
    protected lateinit var client: WebClient

    @Mock
    protected lateinit var gameDataDb: VauhtijuoksuDatabase<GameData>

    @TempDir
    lateinit var tmpDir: File

    protected val username = "vauhtijuoksu"
    protected val password = "vauhtijuoksu"

    // Mockito returns null with any(). This fails on non-nullable parameters
    // Stackoverflow taught me a workaround https://stackoverflow.com/questions/30305217/is-it-possible-to-use-mockito-in-kotlin
    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T

    protected fun <T> any(): T {
        Mockito.any<T>()
        return uninitialized()
    }

    private fun getFreePort(): Int {
        val sock = ServerSocket(0)
        val port = sock.localPort
        sock.close()
        return port
    }

    @BeforeEach
    fun beforeEach() {
        val htpasswdFile = "${tmpDir.path}/.htpasswd"
        val writer = BufferedWriter(FileWriter(File(htpasswdFile)))
        // Pre-generated credentials vauhtijuoksu / vauhtijuoksu
        writer.write("vauhtijuoksu:{SHA}Iih8iFrD8jPkj1eYEw6tJmTbHrg=")
        writer.close()

        val serverPort = getFreePort()
        val injector = Guice.createInjector(
            ApiModule(),
            object : AbstractModule() {
                override fun configure() {
                    bind(object : TypeLiteral<VauhtijuoksuDatabase<GameData>>() {}).toInstance(db)
                    bind(ServerConfiguration::class.java).toInstance(ServerConfiguration(serverPort, htpasswdFile))
                }
            }
        )

        vertx = injector.getInstance(Vertx::class.java)
        server = injector.getInstance(Server::class.java)
        server.start()
        // Vertx is too hasty to claim it's listening
        Thread.sleep(10)
        client = WebClient.create(vertx, WebClientOptions().setDefaultPort(serverPort))
    }

    @AfterEach
    fun afterEach(testContext: VertxTestContext) {
        server.stop()
        client.close()
        vertx.close { testContext.completeNow() }
    }
}
