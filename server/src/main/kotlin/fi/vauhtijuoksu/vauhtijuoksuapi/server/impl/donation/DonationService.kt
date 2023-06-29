package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.donation

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.GeneratedIncentiveCodeDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.MissingEntityException
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GeneratedIncentive
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import jakarta.inject.Inject
import java.util.UUID

data class DonationWithCodes(
    val donation: Donation,
    val incentives: List<GeneratedIncentive>,
)

class DonationService
@Inject constructor(
    private val incentiveCodeDb: GeneratedIncentiveCodeDatabase,
    private val donationDb: VauhtijuoksuDatabase<Donation>,
) {
    fun getDonation(id: UUID): Future<DonationWithCodes> {
        return donationDb.getById(id).map {
            if (it == null) {
                throw MissingEntityException("No such donation")
            }
            it
        }.compose { donation ->
            incentiveCodeDb.getAll().map {
                it.filter { generatedIncentive ->
                    donation.codes.contains(generatedIncentive.generatedCode)
                }
            }.map {
                DonationWithCodes(
                    donation,
                    it,
                )
            }
        }
    }

    fun getDonations(): Future<List<DonationWithCodes>> {
        val incentiveCodes = incentiveCodeDb.getAll()

        return donationDb
            .getAll()
            .flatMap { donations ->
                CompositeFuture.all(
                    donations.map { donation ->
                        incentiveCodes
                            .map {
                                DonationWithCodes(
                                    donation,
                                    it.filter { generatedIncentive ->
                                        donation.codes.contains(generatedIncentive.generatedCode)
                                    },
                                )
                            }
                    },
                )
            }
            .map {
                it.list()
            }
    }
}
