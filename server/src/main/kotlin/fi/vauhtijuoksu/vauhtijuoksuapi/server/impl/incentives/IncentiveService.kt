package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.GeneratedIncentiveCodeDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.MissingEntityException
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GeneratedIncentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.IncentiveCode
import fi.vauhtijuoksu.vauhtijuoksuapi.models.IncentiveType
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import java.util.UUID
import javax.inject.Inject

private data class IncentiveCodeAndShare(
    val code: IncentiveCode,
    val share: Double,
    val parameter: String?,
)

private data class IncentiveAndItsCodes(
    val incentive: Incentive,
    val codes: Set<IncentiveCodeAndShare>,
)

private data class ParameterDonation(
    val sum: Double,
    val parameter: String?
)

private data class IncentiveAndItsParameterDonations(
    val incentive: Incentive,
    val parameterDonation: List<ParameterDonation>,
)

sealed interface IncentiveStatus {
    val type: String
}

enum class MilestoneStatus {
    COMPLETED, INCOMPLETE
}

data class MilestoneIncentiveStatus(
    val status: MilestoneStatus,
    val milestoneGoal: Int,
) : IncentiveStatus {
    override val type: String
        get() = "milestone"
}

data class OptionIncentiveStatus(
    val option: String,
    val amount: Double,
) : IncentiveStatus {
    override val type: String
        get() = "option"
}

data class IncentiveWithStatuses(
    val incentive: Incentive,
    val total: Double,
    val statuses: List<IncentiveStatus>
)

class IncentiveService
@Inject constructor(
    private val incentiveDb: VauhtijuoksuDatabase<Incentive>,
    private val incentiveCodeDb: GeneratedIncentiveCodeDatabase,
    private val donationDb: VauhtijuoksuDatabase<Donation>,
) {
    fun getIncentive(id: UUID): Future<IncentiveWithStatuses> {
        val donations = donationDb.getAll()
        val generatedIncentives = incentiveCodeDb.getAll()
        return incentiveDb.getById(id).flatMap(this::failOnNull)
            .flatMap { mapStatuses(it, donations, generatedIncentives) }
    }

    fun getIncentives(): Future<List<IncentiveWithStatuses>> {
        val donations = donationDb.getAll()
        val generatedIncentives = incentiveCodeDb.getAll()
        return incentiveDb
            .getAll()
            .flatMap {
                CompositeFuture.all(
                    it.map { incentive ->
                        mapStatuses(incentive, donations, generatedIncentives)
                    }
                )
                    .map { compositeFuture ->
                        compositeFuture.list()
                    }
            }
    }

    private fun mapStatuses(
        incentive: Incentive,
        donations: Future<List<Donation>>,
        generatedIncentives: Future<List<GeneratedIncentive>>
    ): Future<IncentiveWithStatuses> {
        return Future.succeededFuture(incentive)
            .compose { getIncentiveCodesForIncentive(incentive, generatedIncentives) }
            .compose { fetchDonationSumsForIncentive(it, donations) }
            .compose(this::calculateTotalAndSummary)
    }

    private fun failOnNull(incentive: Incentive?): Future<Incentive> {
        return if (incentive == null) {
            Future.failedFuture(MissingEntityException("No such incentive"))
        } else {
            Future.succeededFuture(incentive)
        }
    }

    private fun getIncentiveCodesForIncentive(
        incentive: Incentive,
        generatedIncentives: Future<List<GeneratedIncentive>>
    ): Future<IncentiveAndItsCodes> {
        return generatedIncentives.map {
            val incentiveCodes = it.filter { incentiveWithCode ->
                incentiveWithCode.chosenIncentives.any { chosenIncentive ->
                    chosenIncentive.incentiveId == incentive.id
                }
            }
            val inc = IncentiveAndItsCodes(
                incentive,
                incentiveCodes.flatMap { incentiveCode ->
                    val incentiveCodesWithShares = mutableListOf<IncentiveCodeAndShare>()
                    for (chosenIncentive in incentiveCode.chosenIncentives) {
                        incentiveCodesWithShares.add(
                            IncentiveCodeAndShare(
                                incentiveCode.generatedCode,
                                1.0 / incentiveCode.chosenIncentives.size,
                                chosenIncentive.parameter,
                            )
                        )
                    }
                    incentiveCodesWithShares
                }.toSet(),
            )
            return@map inc
        }
    }

    private fun fetchDonationSumsForIncentive(
        incentiveAndItsCodes: IncentiveAndItsCodes,
        donations: Future<List<Donation>>,
    ): Future<IncentiveAndItsParameterDonations> {
        if (incentiveAndItsCodes.codes.isEmpty()) {
            return Future.succeededFuture(
                IncentiveAndItsParameterDonations(
                    incentiveAndItsCodes.incentive, listOf()
                )
            )
        }
        return donations.map { donationList ->
            donationList.flatMap { donation ->
                val foundCodes = incentiveAndItsCodes.codes.filter {
                    donation.codes.contains(it.code)
                }
                if (foundCodes.isEmpty()) {
                    listOf()
                } else {
                    donation.codes.flatMap { code ->
                        incentiveAndItsCodes.codes.filter {
                            it.code == code
                        }.map {
                            ParameterDonation(
                                (donation.amount / donation.codes.size).toDouble() * it.share,
                                it.parameter,
                            )
                        }
                    }
                }
            }
        }.map {
            IncentiveAndItsParameterDonations(
                incentiveAndItsCodes.incentive,
                it,
            )
        }
    }

    private fun calculateTotalAndSummary(
        incentiveAndItsParameterDonations: IncentiveAndItsParameterDonations
    ): Future<IncentiveWithStatuses> {
        val incentive = incentiveAndItsParameterDonations.incentive
        val parameterDonation = incentiveAndItsParameterDonations.parameterDonation

        return when (incentive.type) {
            IncentiveType.MILESTONE -> {
                val sum =
                    if (parameterDonation.isEmpty()) 0.0 else parameterDonation.map { it.sum }.reduce(Double::plus)
                val statuses = mutableListOf<MilestoneIncentiveStatus>()
                for (milestone in incentive.milestones!!.sorted()) {
                    statuses.add(
                        MilestoneIncentiveStatus(
                            if (milestone.toDouble() <= sum) MilestoneStatus.COMPLETED else MilestoneStatus.INCOMPLETE,
                            milestone,
                        )
                    )
                }
                Future.succeededFuture(
                    IncentiveWithStatuses(
                        incentive,
                        sum,
                        statuses,
                    )
                )
            }
            IncentiveType.OPTION -> {
                val options = incentive.optionParameters!!.map { it }
                calculateOptionIncentiveStatuses(incentive, options, parameterDonation)
            }
            IncentiveType.OPEN -> {
                val options = parameterDonation.mapNotNull { it.parameter }.distinct()
                calculateOptionIncentiveStatuses(incentive, options, parameterDonation)
            }
        }
    }

    private fun calculateOptionIncentiveStatuses(
        incentive: Incentive,
        options: List<String>,
        parameterDonation: List<ParameterDonation>
    ): Future<IncentiveWithStatuses> {
        var total = 0.0
        val statuses = mutableListOf<OptionIncentiveStatus>()
        options.forEach { option ->
            val sum =
                parameterDonation.filter { option == it.parameter }.map { it.sum }.reduceOrNull(Double::plus) ?: 0.0
            statuses.add(
                OptionIncentiveStatus(
                    option,
                    sum,
                )
            )
            total = total.plus(sum)
        }
        return Future.succeededFuture(
            IncentiveWithStatuses(
                incentive, total, statuses
            )
        )
    }
}
