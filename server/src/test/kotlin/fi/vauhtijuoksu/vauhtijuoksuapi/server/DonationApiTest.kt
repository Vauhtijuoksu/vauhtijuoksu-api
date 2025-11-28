package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.vauhtijuoksu.vauhtijuoksuapi.MockitoUtils.Companion.any
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.MissingEntityException
import fi.vauhtijuoksu.vauhtijuoksuapi.models.ChosenIncentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GeneratedIncentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.IncentiveCode
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.donation.DonationApiModel
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestDonation.Companion.donation1
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestDonation.Companion.donation2
import io.vertx.core.Future
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import java.util.UUID

class DonationApiTest : ServerTestBase() {
    @Test
    fun testDonationsOptions(testContext: VertxTestContext) {
        client
            .request(HttpMethod.OPTIONS, "/donations")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    val allowedMethods = setOf("GET", "POST", "PATCH", "OPTIONS", "DELETE")
                    assertEquals(
                        allowedMethods,
                        res
                            .headers()
                            .get("Allow")
                            .split(", ")
                            .toSet(),
                    )
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testSingleDonationOptions(testContext: VertxTestContext) {
        client
            .request(HttpMethod.OPTIONS, "/donations/${UUID.randomUUID()}")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    val allowedMethods = setOf("GET", "POST", "PATCH", "OPTIONS", "DELETE")
                    assertEquals(
                        allowedMethods,
                        res
                            .headers()
                            .get("Allow")
                            .split(", ")
                            .toSet(),
                    )
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testGetDonationNoData(testContext: VertxTestContext) {
        `when`(donationDb.getAll()).thenReturn(Future.succeededFuture(ArrayList()))
        `when`(generatedIncentiveCodeDatabase.getAll()).thenReturn(Future.succeededFuture(listOf()))
        client
            .get("/donations")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals("application/json", res.getHeader("content-type"))
                    assertEquals("[]", res.bodyAsString())
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testGetDonation(testContext: VertxTestContext) {
        `when`(donationDb.getAll()).thenReturn(Future.succeededFuture(arrayListOf(donation1, donation2)))
        `when`(generatedIncentiveCodeDatabase.getAll()).thenReturn(Future.succeededFuture(listOf()))
        client
            .get("/donations")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals("application/json", res.getHeader("content-type"))
                    val expectedJson =
                        jacksonObjectMapper().writeValueAsString(
                            arrayListOf(
                                DonationApiModel.fromDonation(donation1),
                                DonationApiModel.fromDonation(donation2),
                            ),
                        )
                    assertEquals(expectedJson, res.bodyAsString())
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testDonationDbError(testContext: VertxTestContext) {
        `when`(donationDb.getAll()).thenReturn(Future.failedFuture(RuntimeException("DB error")))
        client
            .get("/donations")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(500, res.statusCode())
                    verify(donationDb).getAll()
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testAddingDonation(testContext: VertxTestContext) {
        `when`(donationDb.add(any())).thenReturn(Future.succeededFuture())
        val body = JsonObject.mapFrom(DonationApiModel.fromDonation(donation1))
        body.remove("id")
        body.remove("incentives")
        client
            .post("/donations")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(body)
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(201, res.statusCode())
                    val resJson = res.bodyAsJsonObject()
                    assertEquals(
                        DonationApiModel.fromDonation(donation1.copy(id = UUID.fromString(resJson.getString("id")))),
                        res.bodyAsJson(DonationApiModel::class.java),
                    )
                    verify(donationDb).add(any())
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testAddingDonationWithIdFails(testContext: VertxTestContext) {
        client
            .post("/donations")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject.mapFrom(donation1))
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(400, res.statusCode())
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @ParameterizedTest
    @ValueSource(strings = ["name", "amount", "timestamp"])
    fun testMandatoryFieldsAreRequiredWhenAddingDonation(
        missingField: String,
        testContext: VertxTestContext,
    ) {
        val json = JsonObject.mapFrom(DonationApiModel.fromDonation(donation1))
        json.remove("id")
        assertNotNull(json.remove(missingField))
        client
            .post("/donations")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(json)
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(400, res.statusCode())
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testAddingDonationWithoutBodyFails(testContext: VertxTestContext) {
        client
            .post("/donations")
            .authentication(UsernamePasswordCredentials(username, password))
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(400, res.statusCode())
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testAddingDonationWithoutAuthenticationFails(testContext: VertxTestContext) {
        client
            .post("/donations")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(401, res.statusCode())
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testGetSingleDonationNotFound(testContext: VertxTestContext) {
        `when`(donationDb.getById(any())).thenReturn(Future.succeededFuture())
        client
            .get("/donations/${UUID.randomUUID()}")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(404, res.statusCode())
                    assertEquals("application/json", res.getHeader("content-type"))
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testGetSingleDonation(testContext: VertxTestContext) {
        val code = IncentiveCode.random()
        val incentiveId = UUID.randomUUID()
        `when`(donationDb.getById(donation1.id)).thenReturn(Future.succeededFuture(donation1.copy(message = code.code)))
        `when`(generatedIncentiveCodeDatabase.getAll()).thenReturn(
            Future.succeededFuture(
                listOf(
                    GeneratedIncentive(
                        code,
                        listOf(
                            ChosenIncentive(
                                incentiveId,
                                "kissa",
                            ),
                        ),
                    ),
                ),
            ),
        )
        client
            .get("/donations/${donation1.id}")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals("application/json", res.getHeader("content-type"))
                    val donationIncentive =
                        JsonObject().put("incentive_id", incentiveId.toString()).put("parameter", "kissa")
                    val incentive =
                        JsonObject()
                            .put("code", code.code)
                            .put("chosen_incentives", JsonArray().add(donationIncentive))
                    val incentives = JsonArray().add(incentive)
                    val expectedDonation =
                        JsonObject(jacksonObjectMapper().writeValueAsString(DonationApiModel.fromDonation(donation1)))
                            .put("message", code.code)
                            .put("incentives", incentives)
                    assertEquals(
                        expectedDonation,
                        JsonObject(res.bodyAsString()),
                    )
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testPatchDonation(testContext: VertxTestContext) {
        val newDonation =
            donation1.copy(
                read = true,
                message = null,
            )

        `when`(donationDb.getById(donation1.id)).thenReturn(
            Future.succeededFuture(donation1),
            Future.succeededFuture(newDonation),
        )
        `when`(donationDb.update(any())).thenReturn(Future.succeededFuture())
        client
            .patch("/donations/${donation1.id}")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject().put("read", true).put("message", null))
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    println(res.bodyAsString())
                    assertEquals(200, res.statusCode())
                    assertEquals(
                        DonationApiModel.fromDonation(newDonation),
                        res.bodyAsJson(DonationApiModel::class.java),
                    )
                    verify(donationDb).update(newDonation)
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testPatchDonationWithIllegalInput(testContext: VertxTestContext) {
        // Jackson updating modifies the underlying object, even though it's supposed to be immutable
        // Copy the object here, since otherwise the modified state leaks to other tests
        `when`(donationDb.getById(donation1.id)).thenReturn(Future.succeededFuture(donation1.copy()))
        client
            .patch("/donations/${donation1.id}")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject().put("name", null))
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(400, res.statusCode())
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testPatchDonationWithUnknownFields(testContext: VertxTestContext) {
        `when`(donationDb.getById(donation1.id)).thenReturn(Future.succeededFuture(donation1))
        client
            .patch("/donations/${donation1.id}")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject().put("new_field", "value"))
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(400, res.statusCode())
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testPatchWithoutAuthenticationFails(testContext: VertxTestContext) {
        client
            .patch("/donations/${UUID.randomUUID()}")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(401, res.statusCode())
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testSingleDonationDbError(testContext: VertxTestContext) {
        `when`(donationDb.getById(any())).thenReturn(Future.failedFuture(RuntimeException("DB error")))
        client
            .get("/donations/${UUID.randomUUID()}")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(500, res.statusCode())
                    verify(donationDb).getById(any())
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testDeleteDonation(testContext: VertxTestContext) {
        val uuid = UUID.randomUUID()
        `when`(donationDb.delete(any())).thenReturn(Future.succeededFuture())
        client
            .delete("/donations/$uuid")
            .authentication(UsernamePasswordCredentials(username, password))
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(204, res.statusCode())
                    verify(donationDb).delete(uuid)
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testDeleteNonExistingDonation(testContext: VertxTestContext) {
        val uuid = UUID.randomUUID()
        `when`(donationDb.delete(any())).thenReturn(Future.failedFuture(MissingEntityException("No such donation")))
        client
            .delete("/donations/$uuid")
            .authentication(UsernamePasswordCredentials(username, password))
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(404, res.statusCode())
                    verify(donationDb).delete(uuid)
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testDeleteWithoutAuthenticationFails(testContext: VertxTestContext) {
        client
            .delete("/donations/${UUID.randomUUID()}")
            .send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(401, res.statusCode())
                    verify(donationDb, times(0)).delete(any())
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }
}
