package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.vauhtijuoksu.vauhtijuoksuapi.MockitoUtils.Companion.any
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.MissingEntityException
import fi.vauhtijuoksu.vauhtijuoksuapi.models.ParticipantRole
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.gamedata.GameDataApiModel
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestGameData.Companion.gameData1
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestGameData.Companion.gameData2
import io.vertx.core.Future
import io.vertx.core.http.HttpMethod
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
    fun testGamedataOptions(testContext: VertxTestContext) {
        client.request(HttpMethod.OPTIONS, "/gamedata").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    val allowedMethods = setOf("GET", "POST", "PATCH", "OPTIONS", "DELETE")
                    assertEquals(allowedMethods, res.headers().get("Allow").split(", ").toSet())
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testSingleGameDataOptions(testContext: VertxTestContext) {
        client.request(HttpMethod.OPTIONS, "/gamedata/${UUID.randomUUID()}").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    val allowedMethods = setOf("GET", "POST", "PATCH", "OPTIONS", "DELETE")
                    assertEquals(allowedMethods, res.headers().get("Allow").split(", ").toSet())
                }
                testContext.completeNow()
            }
    }

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
                    val expectedJson = jacksonObjectMapper().writeValueAsString(
                        arrayListOf(
                            GameDataApiModel.fromGameData(gameData1),
                            GameDataApiModel.fromGameData(gameData2),
                        ),
                    )
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
        `when`(gameDataDb.add(any())).thenReturn(Future.succeededFuture())
        val body = JsonObject.mapFrom(GameDataApiModel.fromGameData(gameData1))
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
                        GameDataApiModel.fromGameData(gameData1.copy(id = UUID.fromString(resJson.getString("id")))),
                        res.bodyAsJson(GameDataApiModel::class.java),
                    )
                    verify(gameDataDb).add(any())
                    verifyNoMoreInteractions(gameDataDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testAddingGameDataWithOriginHeader(testContext: VertxTestContext) {
        `when`(gameDataDb.add(any())).thenReturn(Future.succeededFuture())
        val body = JsonObject.mapFrom(GameDataApiModel.fromGameData(gameData1))
        body.remove("id")
        client.post("/gamedata").putHeader("Origin", allowedOrigin)
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(body)
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(201, res.statusCode())
                    assertEquals(allowedOrigin, res.getHeader("Access-Control-Allow-Origin"))
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
    @ValueSource(strings = ["game", "start_time", "end_time", "category", "device", "published", "players"])
    fun testMandatoryFieldsAreRequiredWhenAddingGameData(missingField: String, testContext: VertxTestContext) {
        val json = JsonObject.mapFrom(GameDataApiModel.fromGameData(gameData1))
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
        `when`(gameDataDb.getById(gameData1.id)).thenReturn(Future.succeededFuture(gameData1))
        client.get("/gamedata/${gameData1.id}").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals("application/json", res.getHeader("content-type"))
                    assertEquals(
                        JsonObject(jacksonObjectMapper().writeValueAsString(GameDataApiModel.fromGameData(gameData1))),
                        JsonObject(res.bodyAsString()),
                    )
                    verifyNoMoreInteractions(gameDataDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testPatchSingleGameData(testContext: VertxTestContext) {
        val oldId = gameData1.id
        val newGame = gameData2.copy(id = oldId)

        `when`(gameDataDb.getById(gameData1.id)).thenReturn(
            Future.succeededFuture(gameData1),
            Future.succeededFuture(newGame),
        )
        `when`(gameDataDb.update(any())).thenReturn(Future.succeededFuture())
        client.patch("/gamedata/${gameData1.id}")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(
                JsonObject()
                    .put("game", newGame.game)
                    .put("start_time", newGame.startTime)
                    .put("end_time", newGame.endTime)
                    .put("category", newGame.category)
                    .put("device", newGame.device)
                    .put("published", newGame.published)
                    .put("vod_link", newGame.vodLink)
                    .put("img_filename", newGame.imgFilename)
                    .put("meta", newGame.meta)
                    .put(
                        "players",
                        newGame.participants.filter { it.role == ParticipantRole.PLAYER }.map { it.participantId },
                    ).put("participants", emptyList<Any>()),
            )
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    println(res.bodyAsString())
                    assertEquals(200, res.statusCode())
                    assertEquals(GameDataApiModel.fromGameData(newGame), res.bodyAsJson(GameDataApiModel::class.java))
                    verify(gameDataDb).update(newGame)
                    verifyNoMoreInteractions(gameDataDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testPatchGameDataWithIllegalInput(testContext: VertxTestContext) {
        `when`(gameDataDb.getById(gameData1.id)).thenReturn(Future.succeededFuture(gameData1.copy()))
        client.patch("/gamedata/${gameData1.id}")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject().put("game", null))
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    println(res.bodyAsString())
                    assertEquals(400, res.statusCode())
                    verifyNoMoreInteractions(gameDataDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testPatchGameDataWithUnknownFields(testContext: VertxTestContext) {
        `when`(gameDataDb.getById(gameData1.id)).thenReturn(Future.succeededFuture(gameData1))
        client.patch("/gamedata/${gameData1.id}")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject().put("wololoo", "ssh"))
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
    fun testPatchWithoutAuthenticationFails(testContext: VertxTestContext) {
        client.patch("/gamedata/${UUID.randomUUID()}")
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
    fun testPatchWithNoneUrlVodFails(testContext: VertxTestContext) {
        `when`(gameDataDb.getById(gameData1.id)).thenReturn(Future.succeededFuture(gameData1))
        client.patch("/gamedata/${gameData1.id}")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject().put("vod_link", "What is love"))
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
        `when`(gameDataDb.delete(uuid)).thenReturn(Future.succeededFuture())
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
        `when`(gameDataDb.delete(uuid)).thenReturn(Future.failedFuture(MissingEntityException("No such row")))
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
