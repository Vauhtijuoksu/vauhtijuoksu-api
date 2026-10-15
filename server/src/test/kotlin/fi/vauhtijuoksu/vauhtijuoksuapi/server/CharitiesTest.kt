package fi.vauhtijuoksu.vauhtijuoksuapi.server

import fi.vauhtijuoksu.vauhtijuoksuapi.MockitoUtils.Companion.any
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.charities.CharityApiModel
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.charities.NewCharityApiModel
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestCharity.Companion.charity1
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestCharity.Companion.charity2
import io.vertx.core.Future
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class CharitiesTest : ServerTestBase() {
    private val charitiesEndpoint = "/charities"

    @Test
    fun `get returns all charities`() = runTest {
        `when`(charityDb.getAll()).thenReturn(Future.succeededFuture(listOf(charity1, charity2)))

        val res = client.get(charitiesEndpoint)
            .send().coAwait()

        assertEquals(200, res.statusCode())
        assertEquals(
            JsonArray()
                .add(JsonObject.mapFrom(CharityApiModel.from(charity1)))
                .add(JsonObject.mapFrom(CharityApiModel.from(charity2))),
            res.bodyAsJsonArray(),
        )
    }

    @Test
    fun `post requires credentials`() = runTest {
        val res = client.post(charitiesEndpoint)
            .send().coAwait()
        assertEquals(401, res.statusCode())
    }

    @Test
    fun `post adds a new charity`() = runTest {
        `when`(charityDb.add(any())).thenReturn(Future.succeededFuture())
        val body = JsonObject.mapFrom(
            NewCharityApiModel(
                charity1.name,
                charity1.url,
                charity1.goal,
                charity1.imgFilename,
            ),
        )

        val res = client.post(charitiesEndpoint)
            .putHeader("Origin", allowedOrigin)
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(body).coAwait()

        assertEquals(201, res.statusCode())
        val responseBody = res.bodyAsJsonObject().mapTo(CharityApiModel::class.java)
        assertEquals(CharityApiModel.from(charity1).copy(id = responseBody.id), responseBody)
        verify(charityDb).add(charity1.copy(id = responseBody.id))
    }

    @Test
    fun `patch modifies existing charity`() = runTest {
        val expectedCharity = charity1.copy(name = "Uusi nimi")
        `when`(charityDb.getById(charity1.id)).thenReturn(Future.succeededFuture(charity1))
        `when`(charityDb.update(expectedCharity)).thenAnswer {
            `when`(charityDb.getById(charity1.id)).thenReturn(Future.succeededFuture(expectedCharity))
            return@thenAnswer Future.succeededFuture<Unit>()
        }

        val res = client.patch("$charitiesEndpoint/${charity1.id}")
            .putHeader("Origin", allowedOrigin)
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject().put("name", "Uusi nimi")).coAwait()

        assertEquals(200, res.statusCode())
        assertEquals(JsonObject.mapFrom(CharityApiModel.from(expectedCharity)), res.bodyAsJsonObject())
        verify(charityDb).update(expectedCharity)
    }
}
