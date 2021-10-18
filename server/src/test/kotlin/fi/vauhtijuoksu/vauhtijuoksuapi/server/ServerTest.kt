package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.TypeLiteral
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.ServerConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestGameData.Companion.gameData1
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestGameData.Companion.gameData2
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations.openMocks
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.ServerSocket
import java.util.UUID

@ExtendWith(VertxExtension::class)
class ServerTest {
    private lateinit var vertx: Vertx
    private lateinit var server: Server
    private lateinit var client: WebClient

    @Mock
    private lateinit var db: VauhtijuoksuDatabase<GameData>

    @TempDir
    lateinit var tmpDir: File

    val username = "vauhtijuoksu"
    val password = "vauhtijuoksu"

    private lateinit var mocks: AutoCloseable

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
        val htpasswdFile = "${tmpDir.path}/.htpasswd"
        val writer = BufferedWriter(FileWriter(File(htpasswdFile)))
        // Pre-generated credentials vauhtijuoksu / vauhtijuoksu
        writer.write("vauhtijuoksu:{SHA}Iih8iFrD8jPkj1eYEw6tJmTbHrg=")
        writer.close()

        val serverPort = getFreePort()
        mocks = openMocks(this)
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
    fun testGetGameDataNoData(testContext: VertxTestContext) {
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
    fun testGetGameData(testContext: VertxTestContext) {
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

    @Test
    fun testAddingGameData(testContext: VertxTestContext) {
        `when`(db.add(any())).thenReturn(Future.succeededFuture(gameData1.copy(UUID.randomUUID())))
        val body = JsonObject.mapFrom(gameData1)
        body.remove("id")
        client.post("/gamedata")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(body)
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(201, res.statusCode())
                    val resJson = res.bodyAsJsonObject()
                    assertEquals(
                        gameData1.copy(id = UUID.fromString(resJson.getString("id"))),
                        res.bodyAsJson(GameData::class.java)
                    )
                    testContext.completeNow()
                }
            }
    }

    @Test
    fun testAddingGameDataWithIdFails(testContext: VertxTestContext) {
        client.post("/gamedata")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject.mapFrom(gameData1))
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(400, res.statusCode())
                }
                testContext.completeNow()
            }
    }

    @ParameterizedTest
    @ValueSource(strings = ["game", "player", "start_time", "end_time", "category", "device", "published"])
    fun testMandatoryFieldsAreRequiredWhenAddingGameData(missingField: String, testContext: VertxTestContext) {
        val json = JsonObject.mapFrom(gameData1)
        json.remove("id")
        assertNotNull(json.remove(missingField))
        client.post("/gamedata")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(json)
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(400, res.statusCode())
                    assertTrue(res.bodyAsString().contains(missingField))
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testAddingGameDataWithoutBodyFails(testContext: VertxTestContext) {
        client.post("/gamedata")
            .authentication(UsernamePasswordCredentials(username, password))
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(400, res.statusCode())
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testAddingGameDataWithoutAuthenticationFails(testContext: VertxTestContext) {
        client.post("/gamedata")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(401, res.statusCode())
                    verify(db, times(0)).delete(any())
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testGetSingleGameDataNotFound(testContext: VertxTestContext) {
        `when`(db.getById(any())).thenReturn(Future.succeededFuture())
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
        `when`(db.getById(gameData1.id!!)).thenReturn(Future.succeededFuture(gameData1))
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

    @Test
    fun testDeleteGameData(testContext: VertxTestContext) {
        val uuid = UUID.randomUUID()
        `when`(db.delete(any())).thenReturn(Future.succeededFuture(true))
        client.delete("/gamedata/$uuid")
            .authentication(UsernamePasswordCredentials(username, password))
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(204, res.statusCode())
                    verify(db).delete(uuid)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testDeleteNonExistingGameData(testContext: VertxTestContext) {
        val uuid = UUID.randomUUID()
        `when`(db.delete(any())).thenReturn(Future.succeededFuture(false))
        client.delete("/gamedata/$uuid")
            .authentication(UsernamePasswordCredentials(username, password))
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(404, res.statusCode())
                    verify(db).delete(uuid)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testDeleteWithoutAuthenticationFails(testContext: VertxTestContext) {
        client.delete("/gamedata/${UUID.randomUUID()}")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(401, res.statusCode())
                    verify(db, times(0)).delete(any())
                }
                testContext.completeNow()
            }
    }
}
