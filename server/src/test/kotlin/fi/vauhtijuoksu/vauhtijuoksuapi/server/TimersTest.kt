package fi.vauhtijuoksu.vauhtijuoksuapi.server

import fi.vauhtijuoksu.vauhtijuoksuapi.MockitoUtils.Companion.any
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.MissingEntityException
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.timers.NewTimerApiModel
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.timers.TimerApiModel
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestTimer.Companion.timer1
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestTimer.Companion.timer2
import io.vertx.core.Future
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class TimersTest : ServerTestBase() {
    private val timersEndpoint = "/timers"

    @Test
    fun `get returns an empty list when db is empty`(testContext: VertxTestContext) {
        `when`(timerDb.getAll()).thenReturn(Future.succeededFuture(listOf()))
        client.get(timersEndpoint)
            .send()
            .onSuccess {
                testContext.verify {
                    assertEquals(200, it.statusCode())
                    assertEquals("application/json", it.headers().get("Content-Type"))
                    assertEquals(JsonArray(), it.bodyAsJsonArray())
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }

    @Test
    fun `get returns all timers in the database`(testContext: VertxTestContext) {
        `when`(timerDb.getAll()).thenReturn(Future.succeededFuture(listOf(timer1, timer2)))

        client.get(timersEndpoint)
            .send()
            .map {
                testContext.verify {
                    assertEquals(200, it.statusCode())
                    assertEquals("application/json", it.headers().get("Content-Type"))
                    assertEquals(
                        JsonArray()
                            .add(JsonObject.mapFrom(TimerApiModel.from(timer1)))
                            .add(JsonObject.mapFrom(TimerApiModel.from(timer2))),
                        it.bodyAsJsonArray(),
                    )
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }

    @Test
    fun `get accepts all origins`(testContext: VertxTestContext) {
        client.get(timersEndpoint)
            .putHeader("Origin", "https://example.com")
            .send()
            .map { res ->
                testContext.verify {
                    assertEquals("*", res.getHeader("Access-Control-Allow-Origin"))
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }

    @Test
    fun `get by id returns a single timer`(testContext: VertxTestContext) {
        `when`(timerDb.getById(timer1.id)).thenReturn(Future.succeededFuture(timer1))

        client.get("$timersEndpoint/${timer1.id}")
            .send()
            .map {
                testContext.verify {
                    assertEquals(200, it.statusCode())
                    assertEquals("application/json", it.headers().get("Content-Type"))
                    assertEquals(
                        JsonObject.mapFrom(TimerApiModel.from(timer1)),
                        it.bodyAsJsonObject(),
                    )
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }

    @Test
    fun `post requires credentials`(testContext: VertxTestContext) {
        client.post(timersEndpoint)
            .send()
            .onSuccess {
                testContext.verify {
                    assertEquals(401, it.statusCode())
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }

    @Test
    fun `post adds a new timer`(testContext: VertxTestContext) {
        `when`(timerDb.add(any())).thenReturn(Future.succeededFuture())
        val body = JsonObject.mapFrom(
            NewTimerApiModel(
                timer1.startTime,
                timer1.endTime,
                timer1.name,
            ),
        )
        client.post(timersEndpoint)
            .putHeader("Origin", "https://vauhtijuoksu.fi")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(body)
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(201, res.statusCode())
                    assertEquals(corsHeaderUrl, res.getHeader("Access-Control-Allow-Origin"))
                    val response = res.bodyAsJsonObject().mapTo(TimerApiModel::class.java)
                    assertEquals(TimerApiModel.from(timer1).copy(id = response.id), response)
                    verify(timerDb).add(timer1.copy(id = response.id))
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `post validates input`(testContext: VertxTestContext) {
        val body = JsonObject.mapFrom(
            NewTimerApiModel(
                timer1.startTime,
                timer1.endTime,
                timer1.name,
            ),
        )

        body.remove("name")
        client.post(timersEndpoint)
            .putHeader("Origin", "https://vauhtijuoksu.fi")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(body)
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(400, res.statusCode())
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `patch requires credentials`(testContext: VertxTestContext) {
        client.patch("$timersEndpoint/${UUID.randomUUID()}")
            .send()
            .onSuccess {
                testContext.verify {
                    assertEquals(401, it.statusCode())
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }

    @Test
    fun `patch modifies existing timer`(testContext: VertxTestContext) {
        val id = timer1.id
        val expectedTimer = timer1.copy(endTime = null)
        `when`(timerDb.getById(id)).thenReturn(Future.succeededFuture(timer1))
        `when`(timerDb.update(expectedTimer)).thenAnswer {
            `when`(timerDb.getById(expectedTimer.id)).thenReturn(Future.succeededFuture(expectedTimer))
            return@thenAnswer Future.succeededFuture<Void>()
        }

        client.patch("$timersEndpoint/$id")
            .putHeader("Origin", "https://vauhtijuoksu.fi")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject().put("end_time", null))
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals(corsHeaderUrl, res.getHeader("Access-Control-Allow-Origin"))
                    assertEquals(
                        JsonObject.mapFrom(TimerApiModel.from(expectedTimer)),
                        res.bodyAsJsonObject(),
                    )
                    verify(timerDb).update(expectedTimer)
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }

    @Test
    fun `patch returns 404 when the timer does not exists`(testContext: VertxTestContext) {
        `when`(timerDb.getById(any())).thenReturn(Future.failedFuture(MissingEntityException("No such row")))
        client.patch("$timersEndpoint/${UUID.randomUUID()}")
            .putHeader("Origin", "https://vauhtijuoksu.fi")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject().put("end_time", null))
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(404, res.statusCode())
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }

    @Test
    fun `patch validates input`(testContext: VertxTestContext) {
        val id = timer1.id
        `when`(timerDb.getById(id)).thenReturn(Future.succeededFuture(timer1))
        client.patch("$timersEndpoint/$id")
            .putHeader("Origin", "https://vauhtijuoksu.fi")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject().put("name", null))
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(400, res.statusCode())
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }
}
