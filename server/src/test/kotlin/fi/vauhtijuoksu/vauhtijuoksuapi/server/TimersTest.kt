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
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class TimersTest : ServerTestBase() {
    private val timersEndpoint = "/timers"

    @Test
    fun `get returns an empty list when db is empty`() = runTest {
        `when`(timerDb.getAll()).thenReturn(Future.succeededFuture(listOf()))
        val res = client.get(timersEndpoint)
            .send().await()
        assertEquals(200, res.statusCode())
        assertEquals("application/json", res.headers().get("Content-Type"))
        assertEquals(JsonArray(), res.bodyAsJsonArray())
    }

    @Test
    fun `get returns all timers in the database`() = runTest {
        `when`(timerDb.getAll()).thenReturn(Future.succeededFuture(listOf(timer1, timer2)))

        val res = client.get(timersEndpoint)
            .send().await()

        assertEquals(200, res.statusCode())
        assertEquals("application/json", res.headers().get("Content-Type"))
        assertEquals(
            JsonArray()
                .add(JsonObject.mapFrom(TimerApiModel.from(timer1)))
                .add(JsonObject.mapFrom(TimerApiModel.from(timer2))),
            res.bodyAsJsonArray(),
        )
    }

    @Test
    fun `get accepts all origins`() = runTest {
        val res = client.get(timersEndpoint)
            .putHeader("Origin", "https://example.com")
            .send().await()
        assertEquals("*", res.getHeader("Access-Control-Allow-Origin"))
    }

    @Test
    fun `get by id returns a single timer`() = runTest {
        `when`(timerDb.getById(timer1.id)).thenReturn(Future.succeededFuture(timer1))

        val res = client.get("$timersEndpoint/${timer1.id}")
            .send().await()
        assertEquals(200, res.statusCode())
        assertEquals("application/json", res.headers().get("Content-Type"))
        assertEquals(
            JsonObject.mapFrom(TimerApiModel.from(timer1)),
            res.bodyAsJsonObject(),
        )
    }

    @Test
    fun `post requires credentials`() = runTest {
        val res = client.post(timersEndpoint)
            .send().await()
        assertEquals(401, res.statusCode())
    }

    @Test
    fun `post adds a new timer`() = runTest {
        `when`(timerDb.add(any())).thenReturn(Future.succeededFuture())
        val body = JsonObject.mapFrom(
            NewTimerApiModel(
                timer1.startTime,
                timer1.endTime,
                timer1.name,
            ),
        )
        val res = client.post(timersEndpoint)
            .putHeader("Origin", allowedOrigin)
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(body).coAwait()
        assertEquals(201, res.statusCode())
        assertEquals(allowedOrigin, res.getHeader("Access-Control-Allow-Origin"))

        val responseBody = res.bodyAsJsonObject().mapTo(TimerApiModel::class.java)
        assertEquals(TimerApiModel.from(timer1).copy(id = responseBody.id), responseBody)
        verify(timerDb).add(timer1.copy(id = responseBody.id))
    }

    @Test
    fun `post validates input`() = runTest {
        val body = JsonObject.mapFrom(
            NewTimerApiModel(
                timer1.startTime,
                timer1.endTime,
                timer1.name,
            ),
        )

        body.remove("name")
        val res = client.post(timersEndpoint)
            .putHeader("Origin", allowedOrigin)
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(body).await()

        assertEquals(400, res.statusCode())
    }

    @Test
    fun `patch requires credentials`() = runTest {
        val res = client.patch("$timersEndpoint/${UUID.randomUUID()}")
            .send().await()

        assertEquals(401, res.statusCode())
    }

    @Test
    fun `patch modifies existing timer`() = runTest {
        val id = timer1.id
        val expectedTimer = timer1.copy(endTime = null)
        `when`(timerDb.getById(id)).thenReturn(Future.succeededFuture(timer1))
        `when`(timerDb.update(expectedTimer)).thenAnswer {
            `when`(timerDb.getById(expectedTimer.id)).thenReturn(Future.succeededFuture(expectedTimer))
            return@thenAnswer Future.succeededFuture<Void>()
        }

        val res = client.patch("$timersEndpoint/$id")
            .putHeader("Origin", allowedOrigin)
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject().put("end_time", null)).await()

        assertEquals(200, res.statusCode())
        assertEquals(allowedOrigin, res.getHeader("Access-Control-Allow-Origin"))
        assertEquals(
            JsonObject.mapFrom(TimerApiModel.from(expectedTimer)),
            res.bodyAsJsonObject(),
        )
        verify(timerDb).update(expectedTimer)
    }

    @Test
    fun `patch returns 404 when the timer does not exists`() = runTest {
        `when`(timerDb.getById(any())).thenReturn(Future.failedFuture(MissingEntityException("No such row")))
        val res = client.patch("$timersEndpoint/${UUID.randomUUID()}")
            .putHeader("Origin", "https://vauhtijuoksu.fi")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject().put("end_time", null)).await()

        assertEquals(404, res.statusCode())
    }

    @Test
    fun `patch validates input`() = runTest {
        val id = timer1.id
        `when`(timerDb.getById(id)).thenReturn(Future.succeededFuture(timer1))
        val res = client.patch("$timersEndpoint/$id")
            .putHeader("Origin", allowedOrigin)
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject().put("name", null)).await()
        assertEquals(400, res.statusCode())
    }
}
