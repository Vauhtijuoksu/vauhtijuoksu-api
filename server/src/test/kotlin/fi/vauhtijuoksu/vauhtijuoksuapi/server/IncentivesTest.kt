package fi.vauhtijuoksu.vauhtijuoksuapi.server

import fi.vauhtijuoksu.vauhtijuoksuapi.MockitoUtils.Companion.any
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives.IncentiveApiModel
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives.IncentiveWithStatuses
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives.MilestoneIncentiveStatus
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives.MilestoneStatus
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives.NewIncentiveApiModel
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestIncentive
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

class IncentivesTest : ServerTestBase() {
    private val incentivesEndpoint = "/incentives"
    private val someIncentive = TestIncentive.incentive1

    @Test
    fun `get returns an empty list when db is empty`(testContext: VertxTestContext) {
        `when`(incentiveDatabase.getAll()).thenReturn(Future.succeededFuture(listOf()))
        `when`(donationDb.getAll()).thenReturn(Future.succeededFuture(listOf()))
        `when`(generatedIncentiveCodeDatabase.getAll()).thenReturn(Future.succeededFuture(listOf()))
        client.get(incentivesEndpoint)
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
    fun `get returns all incentives in the database`(testContext: VertxTestContext) {
        val incentivesInDb =
            listOf(someIncentive.copy(id = UUID.randomUUID()), someIncentive.copy(id = UUID.randomUUID()))
        `when`(incentiveDatabase.getAll()).thenReturn(Future.succeededFuture(incentivesInDb))
        `when`(donationDb.getAll()).thenReturn(Future.succeededFuture(listOf()))
        `when`(generatedIncentiveCodeDatabase.getAll()).thenReturn(Future.succeededFuture(listOf()))

        client.get(incentivesEndpoint)
            .send()
            .onSuccess {
                testContext.verify {
                    assertEquals(200, it.statusCode())
                    assertEquals("application/json", it.headers().get("Content-Type"))
                    assertEquals(
                        JsonArray(
                            incentivesInDb
                                .map {
                                    IncentiveApiModel.fromIncentiveWithStatuses(
                                        IncentiveWithStatuses(
                                            it,
                                            0.0,
                                            listOf(
                                                MilestoneIncentiveStatus(
                                                    MilestoneStatus.INCOMPLETE,
                                                    100
                                                )
                                            ),
                                        )
                                    )
                                }
                                .map { it.toJson() }
                        ),
                        it.bodyAsJsonArray()
                    )
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }

    @Test
    fun `get accepts all origins`(testContext: VertxTestContext) {
        client.get(incentivesEndpoint)
            .putHeader("Origin", "https://example.com")
            .send()
            .onSuccess { res ->
                testContext.verify {
                    assertEquals("*", res.getHeader("Access-Control-Allow-Origin"))
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }

    @Test
    fun `get by id returns a single incentive`(testContext: VertxTestContext) {
        `when`(incentiveDatabase.getById(someIncentive.id)).thenReturn(Future.succeededFuture(someIncentive))
        `when`(donationDb.getAll()).thenReturn(Future.succeededFuture(listOf()))
        `when`(generatedIncentiveCodeDatabase.getAll()).thenReturn(Future.succeededFuture(listOf()))

        client.get("$incentivesEndpoint/${someIncentive.id}")
            .send()
            .onSuccess {
                testContext.verify {
                    assertEquals(200, it.statusCode())
                    assertEquals("application/json", it.headers().get("Content-Type"))
                    assertEquals(
                        IncentiveApiModel.fromIncentiveWithStatuses(
                            IncentiveWithStatuses(
                                someIncentive,
                                0.0,
                                listOf(
                                    MilestoneIncentiveStatus(
                                        MilestoneStatus.INCOMPLETE,
                                        100
                                    )
                                ),
                            )
                        ).toJson(),
                        it.bodyAsJsonObject()
                    )
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }

    @Test
    fun `post requires credentials`(testContext: VertxTestContext) {
        client.post(incentivesEndpoint)
            .send()
            .onSuccess {
                testContext.verify {
                    assertEquals(401, it.statusCode())
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }

    @Test
    fun `post adds a new incentive`(testContext: VertxTestContext) {
        `when`(incentiveDatabase.add(any())).thenAnswer {
            Future.succeededFuture(it.getArgument<Incentive>(0))
        }
        val body = JsonObject.mapFrom(
            NewIncentiveApiModel(
                someIncentive.gameId,
                someIncentive.title,
                someIncentive.endTime,
                someIncentive.type.name,
                someIncentive.info,
                someIncentive.milestones,
                someIncentive.optionParameters,
                someIncentive.openCharLimit,
            )
        )
        client.post(incentivesEndpoint)
            .putHeader("Origin", "https://vauhtijuoksu.fi")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(body)
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(201, res.statusCode())
                    assertEquals(corsHeaderUrl, res.getHeader("Access-Control-Allow-Origin"))
                    val response = res.bodyAsJsonObject().mapTo(IncentiveApiModel::class.java)
                    assertEquals(IncentiveApiModel.fromIncentive(someIncentive).copy(id = response.id), response)
                    verify(incentiveDatabase).add(someIncentive.copy(id = response.id))
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `post validates input`(testContext: VertxTestContext) {
        val body = JsonObject.mapFrom(
            NewIncentiveApiModel(
                someIncentive.gameId,
                someIncentive.title,
                someIncentive.endTime,
                someIncentive.type.name,
                someIncentive.info,
                someIncentive.milestones,
                someIncentive.optionParameters,
                someIncentive.openCharLimit,
            )
        )
        body.remove("title")
        client.post(incentivesEndpoint)
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
        client.patch("$incentivesEndpoint/${UUID.randomUUID()}")
            .send()
            .onSuccess {
                testContext.verify {
                    assertEquals(401, it.statusCode())
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }

    @Test
    fun `patch modifies existing incentive`(testContext: VertxTestContext) {
        val id = someIncentive.id
        val expectedIncentive = someIncentive.copy(endTime = null)
        `when`(incentiveDatabase.getById(id)).thenReturn(Future.succeededFuture(someIncentive))
        `when`(incentiveDatabase.update(any())).thenReturn(Future.succeededFuture(expectedIncentive))
        client.patch("$incentivesEndpoint/$id")
            .putHeader("Origin", "https://vauhtijuoksu.fi")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject().put("end_time", null))
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals(corsHeaderUrl, res.getHeader("Access-Control-Allow-Origin"))
                    assertEquals(
                        JsonObject.mapFrom(IncentiveApiModel.fromIncentive(expectedIncentive)),
                        res.bodyAsJsonObject()
                    )
                    verify(incentiveDatabase).update(expectedIncentive)
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }

    @Test
    fun `patch returns 404 when the incentive does not exists`(testContext: VertxTestContext) {
        `when`(incentiveDatabase.getById(any())).thenReturn(Future.succeededFuture())
        client.patch("$incentivesEndpoint/${UUID.randomUUID()}")
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
        val id = someIncentive.id
        `when`(incentiveDatabase.getById(id)).thenReturn(Future.succeededFuture(someIncentive))
        client.patch("$incentivesEndpoint/$id")
            .putHeader("Origin", "https://vauhtijuoksu.fi")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject().put("title", null))
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(400, res.statusCode())
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }
}
