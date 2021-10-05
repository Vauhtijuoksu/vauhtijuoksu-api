package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.inject.AbstractModule
import com.google.inject.Guice
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import io.vertx.core.Future
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
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations.openMocks
import java.time.Instant
import java.util.*

@ExtendWith(VertxExtension::class)
class ServerTest {
    private lateinit var vertx: Vertx
    private lateinit var server: Server
    private lateinit var client: WebClient

    @Mock
    private lateinit var db: VauhtijuoksuDatabase

    private var mocks: AutoCloseable? = null

    @BeforeEach
    fun beforeEach() {
        mocks = openMocks(this)
        val injector = Guice.createInjector(
            ApiModule(),
            object : AbstractModule() {
                override fun configure() {
                    bind(VauhtijuoksuDatabase::class.java).toInstance(db)
                }
            }
        )

        vertx = injector.getInstance(Vertx::class.java)
        server = injector.getInstance(Server::class.java)
        server.start()
        client = WebClient.create(vertx, WebClientOptions().setDefaultPort(8080))
    }

    @AfterEach
    fun afterEach(testContext: VertxTestContext) {
        mocks?.close()
        server.stop()
        client.close()
        vertx.close { testContext.completeNow() }
    }

    @Test
    fun testServerResponds(testContext: VertxTestContext) {
        client.get("/").send().onComplete { res ->
            testContext.verify {
                assertTrue(res.succeeded())
                assertEquals(404, res.result().statusCode())
            }
            testContext.completeNow()
        }
    }

    @Test
    fun testGamedataNoData(testContext: VertxTestContext) {
        `when`(db.getAll()).thenReturn(Future.succeededFuture(ArrayList()))
        client.get("/gamedata").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals("[]", res.bodyAsString())
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testGamedata(testContext: VertxTestContext) {
        val gameData1 = GameData(
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
        val gameData2 = GameData(
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

        `when`(db.getAll()).thenReturn(Future.succeededFuture(arrayListOf(gameData1, gameData2)))
        client.get("/gamedata").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    val expectedJson = jacksonObjectMapper().writeValueAsString(arrayListOf(gameData1, gameData2))
                    assertEquals(expectedJson, res.bodyAsString())
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testGameDataDbError(testContext: VertxTestContext){
        `when`(db.getAll()).thenReturn(Future.failedFuture(RuntimeException("Vituixmän")))
        client.get("/gamedata").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(500, res.statusCode())
                }
                testContext.completeNow()
            }
    }
}
