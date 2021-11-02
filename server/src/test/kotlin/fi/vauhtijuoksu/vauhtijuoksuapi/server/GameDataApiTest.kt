package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestGameData.Companion.gameData1
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestGameData.Companion.gameData2
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import java.util.UUID

class GameDataApiTest : ServerTestBase() {
    @Test
    fun testGetGameDataNoData(testContext: VertxTestContext) {
        `when`(gameDataDb.getAll()).thenReturn(Future.succeededFuture(ArrayList()))
        client.get("/gamedata").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals("application/json", res.getHeader("content-type"))
                    assertEquals("[]", res.bodyAsString())
                    verifyNoMoreInteractions(gameDataDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testGetGameData(testContext: VertxTestContext) {
        `when`(gameDataDb.getAll()).thenReturn(Future.succeededFuture(arrayListOf(gameData1, gameData2)))
        client.get("/gamedata").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals("application/json", res.getHeader("content-type"))
                    val expectedJson = jacksonObjectMapper().writeValueAsString(arrayListOf(gameData1, gameData2))
                    assertEquals(expectedJson, res.bodyAsString())
                    verifyNoMoreInteractions(gameDataDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testGetGameDataWithOriginHeader(testContext: VertxTestContext) {
        `when`(gameDataDb.getAll()).thenReturn(Future.succeededFuture(arrayListOf(gameData1, gameData2)))
        client.get("/gamedata").putHeader("Origin", "http://localhost").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals("*", res.getHeader("Access-Control-Allow-Origin"))
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testGameDataDbError(testContext: VertxTestContext) {
        `when`(gameDataDb.getAll()).thenReturn(Future.failedFuture(RuntimeException("DB error")))
        client.get("/gamedata").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(500, res.statusCode())
                    verify(gameDataDb).getAll()
                    verifyNoMoreInteractions(gameDataDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testAddingGameData(testContext: VertxTestContext) {
        `when`(gameDataDb.add(any())).thenReturn(Future.succeededFuture(gameData1.copy(UUID.randomUUID())))
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
                    verify(gameDataDb).add(any())
                    verifyNoMoreInteractions(gameDataDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testAddingGameDataWithOriginHeader(testContext: VertxTestContext) {
        `when`(gameDataDb.add(any())).thenReturn(Future.succeededFuture(gameData1.copy(UUID.randomUUID())))
        val body = JsonObject.mapFrom(gameData1)
        body.remove("id")
        client.post("/gamedata").putHeader("Origin", "https://vauhtijuoksu.fi")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(body)
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(201, res.statusCode())
                    assertEquals(corsHeaderUrl, res.getHeader("Access-Control-Allow-Origin"))
                }
                testContext.completeNow()
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
                    verifyNoMoreInteractions(gameDataDb)
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
                    verifyNoMoreInteractions(gameDataDb)
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
                    verifyNoMoreInteractions(gameDataDb)
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
                    verifyNoMoreInteractions(gameDataDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testGetSingleGameDataNotFound(testContext: VertxTestContext) {
        `when`(gameDataDb.getById(any())).thenReturn(Future.succeededFuture())
        client.get("/gamedata/${UUID.randomUUID()}").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(404, res.statusCode())
                    assertEquals("application/json", res.getHeader("content-type"))
                    verifyNoMoreInteractions(gameDataDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testGetSingleGameData(testContext: VertxTestContext) {
        `when`(gameDataDb.getById(gameData1.id!!)).thenReturn(Future.succeededFuture(gameData1))
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
                    verifyNoMoreInteractions(gameDataDb)
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
        `when`(gameDataDb.getById(any())).thenReturn(Future.failedFuture(RuntimeException("DB error")))
        client.get("/gamedata/${UUID.randomUUID()}").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(500, res.statusCode())
                    verify(gameDataDb).getById(any())
                    verifyNoMoreInteractions(gameDataDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testDeleteGameData(testContext: VertxTestContext) {
        val uuid = UUID.randomUUID()
        `when`(gameDataDb.delete(any())).thenReturn(Future.succeededFuture(true))
        client.delete("/gamedata/$uuid")
            .authentication(UsernamePasswordCredentials(username, password))
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(204, res.statusCode())
                    verify(gameDataDb).delete(uuid)
                    verifyNoMoreInteractions(gameDataDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testDeleteNonExistingGameData(testContext: VertxTestContext) {
        val uuid = UUID.randomUUID()
        `when`(gameDataDb.delete(any())).thenReturn(Future.succeededFuture(false))
        client.delete("/gamedata/$uuid")
            .authentication(UsernamePasswordCredentials(username, password))
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(404, res.statusCode())
                    verify(gameDataDb).delete(uuid)
                    verifyNoMoreInteractions(gameDataDb)
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
                    verify(gameDataDb, times(0)).delete(any())
                    verifyNoMoreInteractions(gameDataDb)
                }
                testContext.completeNow()
            }
    }
}
