package fi.vauhtijuoksu.vauhtijuoksuapi.server

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.GeneratedIncentiveCodeDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.VauhtijuoksuException
import fi.vauhtijuoksu.vauhtijuoksuapi.models.ChosenIncentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GeneratedIncentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.IncentiveCode
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.donation.DonationService
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.donation.DonationWithCodes
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestDonation
import fi.vauhtijuoksu.vauhtijuoksuapi.verifyAndCompleteTest
import io.vertx.core.Future
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@ExtendWith(VertxExtension::class)
class DonationServiceTest {
    private lateinit var donationService: DonationService

    @Mock
    private lateinit var incentiveCodeDb: GeneratedIncentiveCodeDatabase

    @Mock
    private lateinit var donationDb: VauhtijuoksuDatabase<Donation>

    @BeforeEach
    fun setup() {
        donationService = DonationService(
            incentiveCodeDb,
            donationDb,
        )
    }

    private val generated1 = GeneratedIncentive(
        IncentiveCode.random(),
        listOf(
            ChosenIncentive(
                UUID.randomUUID(),
                "kissa"
            ),
            ChosenIncentive(
                UUID.randomUUID(),
                null,
            )
        )
    )

    private val generated2 = GeneratedIncentive(
        IncentiveCode.random(),
        listOf(
            ChosenIncentive(
                UUID.randomUUID(),
                "koira"
            ),
            ChosenIncentive(
                UUID.randomUUID(),
                "Fruktoosi",
            )
        )
    )

    private val donationWithGen1 = TestDonation.donation1.copy(message = "Ota rahet: ${generated1.generatedCode}")
    private val donationWithGen2 = TestDonation.donation2.copy(message = "Ota rahet: ${generated2.generatedCode}")

    @Test
    fun `fetching a non-existing donation yields error`(testContext: VertxTestContext) {
        val id = UUID.randomUUID()
        `when`(donationDb.getById(id)).thenReturn(Future.succeededFuture(null))
        donationService
            .getDonation(id)
            .onSuccess { testContext.failNow("Expected to fail") }
            .onFailure {
                testContext.verify {
                    it is VauhtijuoksuException
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `fetching a donation with no codes`(testContext: VertxTestContext) {
        val id = UUID.randomUUID()
        `when`(donationDb.getById(id)).thenReturn(Future.succeededFuture(donationWithGen1))
        `when`(incentiveCodeDb.getAll()).thenReturn(Future.succeededFuture(listOf()))
        donationService
            .getDonation(id)
            .verifyAndCompleteTest(testContext) {
                assertEquals(donationWithGen1, it.donation)
                assertTrue(it.incentives.isEmpty())
            }
    }

    @Test
    fun `fetching a donation with codes`(testContext: VertxTestContext) {
        val id = UUID.randomUUID()
        `when`(donationDb.getById(id)).thenReturn(Future.succeededFuture(donationWithGen1))
        `when`(incentiveCodeDb.getAll()).thenReturn(Future.succeededFuture(listOf(generated1, generated2)))
        donationService
            .getDonation(id)
            .verifyAndCompleteTest(testContext) {
                assertEquals(donationWithGen1, it.donation)
                assertEquals(listOf(generated1), it.incentives)
            }
    }

    @Test
    fun `fetching all donations on empty db`(testContext: VertxTestContext) {
        `when`(donationDb.getAll()).thenReturn(Future.succeededFuture(listOf()))
        `when`(incentiveCodeDb.getAll()).thenReturn(Future.succeededFuture(listOf()))
        donationService
            .getDonations()
            .verifyAndCompleteTest(testContext) {
                assertTrue(it.isEmpty())
            }
    }

    @Test
    fun `fetching all donations with on codes`(testContext: VertxTestContext) {
        `when`(donationDb.getAll()).thenReturn(Future.succeededFuture(listOf(donationWithGen1, donationWithGen2)))
        `when`(incentiveCodeDb.getAll()).thenReturn(Future.succeededFuture(listOf()))
        donationService
            .getDonations()
            .verifyAndCompleteTest(testContext) {
                assertEquals(
                    listOf(
                        DonationWithCodes(donationWithGen1, listOf()),
                        DonationWithCodes(donationWithGen2, listOf())
                    ),
                    it
                )
            }
    }

    @Test
    fun `fetching all donations with codes`(testContext: VertxTestContext) {
        `when`(donationDb.getAll()).thenReturn(Future.succeededFuture(listOf(donationWithGen1, donationWithGen2)))
        `when`(incentiveCodeDb.getAll()).thenReturn(Future.succeededFuture(listOf(generated1, generated2)))
        donationService
            .getDonations()
            .verifyAndCompleteTest(testContext) {
                assertEquals(
                    listOf(
                        DonationWithCodes(donationWithGen1, listOf(generated1)),
                        DonationWithCodes(donationWithGen2, listOf(generated2))
                    ),
                    it
                )
            }
    }
}
