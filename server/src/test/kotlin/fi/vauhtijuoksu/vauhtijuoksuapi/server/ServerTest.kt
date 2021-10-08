package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.inject.AbstractModule
import com.google.inject.Guice
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.ServerConfiguration
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations.openMocks
import java.net.ServerSocket
import java.net.URL
import java.time.Instant
import java.util.Date
import java.util.Optional
import java.util.UUID

@ExtendWith(VertxExtension::class)
class ServerTest {
    private lateinit var vertx: Vertx
    private lateinit var server: Server
    private lateinit var client: WebClient

    @Mock
    private lateinit var db: VauhtijuoksuDatabase

    private lateinit var mocks: AutoCloseable

    private val gameData1 = GameData(
        UUID.randomUUID(),
        "Tetris",
        "jsloth",
        Date.from(Instant.now()),
        Date.from(Instant.now()),
        "any%",
        "PC",
        "1970",
        null,
        "tetris.png",
        "jiisloth"
    )
    private val gameData2 = GameData(
        UUID.randomUUID(),
        "Halo",
        "T3mu & Spike_B",
        Date.from(Instant.now()),
        Date.from(Instant.now()),
        "any%",
        "PC",
        "2004",
        URL("https://youtube.com/video"),
        "halo.png",
        "T3_mu & Spike_B"
    )

    // Mockito returns null with any(). This fails on non-nullable parameters
    // Stackoverflow taught me a workaround https://stackoverflow.com/questions/30305217/is-it-possible-to-use-mockito-in-kotlin
    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T

    private fun <T> any(): T {
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
        val serverPort = getFreePort()
        mocks = openMocks(this)
        val injector = Guice.createInjector(
            ApiModule(),
            object : AbstractModule() {
                override fun configure() {
                    bind(VauhtijuoksuDatabase::class.java).toInstance(db)
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
    fun afterEach(testContext: VertxTestContext) {
        mocks.close()
        server.stop()
        client.close()
        vertx.close { testContext.completeNow() }
    }

    @Test
    fun testServerResponds(testContext: VertxTestContext) {
        client.get("/").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(404, res.statusCode())
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testGameDataNoData(testContext: VertxTestContext) {
        `when`(db.getAll()).thenReturn(Future.succeededFuture(ArrayList()))
        client.get("/gamedata").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals("application/json", res.getHeader("content-type"))
                    assertEquals("[]", res.bodyAsString())
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testGameData(testContext: VertxTestContext) {
        `when`(db.getAll()).thenReturn(Future.succeededFuture(arrayListOf(gameData1, gameData2)))
        client.get("/gamedata").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals("application/json", res.getHeader("content-type"))
                    val expectedJson = jacksonObjectMapper().writeValueAsString(arrayListOf(gameData1, gameData2))
                    assertEquals(expectedJson, res.bodyAsString())
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testPostGameData(testContext: VertxTestContext) {
        // TODO endpoint not yet implemented
        testContext.completeNow()
    }

    @Test
    fun testGameDataDbError(testContext: VertxTestContext) {
        `when`(db.getAll()).thenReturn(Future.failedFuture(RuntimeException("DB error")))
        client.get("/gamedata").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(500, res.statusCode())
                }
                testContext.completeNow()
            }
    }

    @ParameterizedTest
    @ValueSource(strings = ["PUT", "PATCH", "DELETE"])
    fun testGameDataNotAllowedMethods(method: String, testContext: VertxTestContext) {
        client.request(HttpMethod(method), "/gamedata").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(405, res.statusCode())
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testGetSingleGameDataNotFound(testContext: VertxTestContext) {
        `when`(db.getById(any())).thenReturn(Future.succeededFuture(Optional.empty()))
        client.get("/gamedata/${UUID.randomUUID()}").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(404, res.statusCode())
                    assertEquals("application/json", res.getHeader("content-type"))
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testGetSingleGameData(testContext: VertxTestContext) {
        `when`(db.getById(gameData1.id)).thenReturn(Future.succeededFuture(Optional.of(gameData1)))
        client.get("/gamedata/${gameData1.id}").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals("application/json", res.getHeader("content-type"))
                    assertEquals(
                        JsonObject(jacksonObjectMapper().writeValueAsString(gameData1)),
                        JsonObject(res.bodyAsString())
                    )
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testPatchSingleGameData(testContext: VertxTestContext) {
        // TODO endpoint not yet implemented
        testContext.completeNow()
    }

    @Test
    fun testSingleGameDataDbError(testContext: VertxTestContext) {
        `when`(db.getById(any())).thenReturn(Future.failedFuture(RuntimeException("DB error")))
        client.get("/gamedata/${UUID.randomUUID()}").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(500, res.statusCode())
                }
                testContext.completeNow()
            }
    }

    @ParameterizedTest
    @ValueSource(strings = ["POST", "PUT", "DELETE"])
    fun testGameDataByIdNotAllowedMethods(method: String, testContext: VertxTestContext) {
        client.request(HttpMethod(method), "/gamedata/${UUID.randomUUID()}").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(405, res.statusCode())
                }
                testContext.completeNow()
            }
    }
}
