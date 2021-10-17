package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestDonation.Companion.donation1
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestDonation.Companion.donation2
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

class DonationApiTest : ServerTestBase() {
    @Test
    fun testGetDonationNoData(testContext: VertxTestContext) {
        `when`(donationDb.getAll()).thenReturn(Future.succeededFuture(ArrayList()))
        client.get("/donations").send()
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
        client.get("/donations").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals("application/json", res.getHeader("content-type"))
                    val expectedJson = jacksonObjectMapper().writeValueAsString(arrayListOf(donation1, donation2))
                    assertEquals(expectedJson, res.bodyAsString())
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testDonationDbError(testContext: VertxTestContext) {
        `when`(donationDb.getAll()).thenReturn(Future.failedFuture(RuntimeException("DB error")))
        client.get("/donations").send()
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
        `when`(donationDb.add(any())).thenReturn(Future.succeededFuture(donation1.copy(UUID.randomUUID())))
        val body = JsonObject.mapFrom(donation1)
        body.remove("id")
        client.post("/donations")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(body)
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(201, res.statusCode())
                    val resJson = res.bodyAsJsonObject()
                    assertEquals(
                        donation1.copy(id = UUID.fromString(resJson.getString("id"))),
                        res.bodyAsJson(Donation::class.java)
                    )
                    verify(donationDb).add(any())
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testAddingDonationWithIdFails(testContext: VertxTestContext) {
        client.post("/donations")
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
    fun testMandatoryFieldsAreRequiredWhenAddingDonation(missingField: String, testContext: VertxTestContext) {
        val json = JsonObject.mapFrom(donation1)
        json.remove("id")
        assertNotNull(json.remove(missingField))
        client.post("/donations")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(json)
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(400, res.statusCode())
                    assertTrue(res.bodyAsString().contains(missingField))
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testAddingDonationWithoutBodyFails(testContext: VertxTestContext) {
        client.post("/donations")
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
        client.post("/donations")
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
        client.get("/donations/${UUID.randomUUID()}").send()
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
        `when`(donationDb.getById(donation1.id!!)).thenReturn(Future.succeededFuture(donation1))
        client.get("/donations/${donation1.id}").send()
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(200, res.statusCode())
                    assertEquals("application/json", res.getHeader("content-type"))
                    assertEquals(
                        JsonObject(jacksonObjectMapper().writeValueAsString(donation1)),
                        JsonObject(res.bodyAsString())
                    )
                    verifyNoMoreInteractions(donationDb)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testPatchSingleDonation(testContext: VertxTestContext) {
        // TODO endpoint not yet implemented
        testContext.completeNow()
    }

    @Test
    fun testSingleDonationDbError(testContext: VertxTestContext) {
        `when`(donationDb.getById(any())).thenReturn(Future.failedFuture(RuntimeException("DB error")))
        client.get("/donations/${UUID.randomUUID()}").send()
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
        `when`(donationDb.delete(any())).thenReturn(Future.succeededFuture(true))
        client.delete("/donations/$uuid")
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
        `when`(donationDb.delete(any())).thenReturn(Future.succeededFuture(false))
        client.delete("/donations/$uuid")
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
        client.delete("/donations/${UUID.randomUUID()}")
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
