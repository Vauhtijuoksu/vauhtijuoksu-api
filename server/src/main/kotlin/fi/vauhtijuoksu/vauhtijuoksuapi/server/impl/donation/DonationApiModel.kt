package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.donation

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiModel
import io.vertx.core.json.JsonObject
import java.util.Date
import java.util.UUID

data class ChosenIncentiveInDonationApi(
    @JsonProperty("incentive_id")
    val incentiveId: UUID,
    val parameter: String?,
)

data class IncentiveInDonationApi(
    val code: String,
    @JsonProperty("chosen_incentives")
    val chosenIncentives: List<ChosenIncentiveInDonationApi>,
)

data class DonationApiModel(
    val id: UUID,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val timestamp: Date,
    val name: String,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val message: String?,
    val amount: Float,
    val read: Boolean = false,
    @JsonProperty("external_id")
    val externalId: String?,
    val incentives: List<IncentiveInDonationApi>,
) :
    ApiModel<Donation> {
    override fun toModel(): Donation {
        return Donation(
            id,
            timestamp,
            name,
            message,
            amount,
            read,
            externalId,
        )
    }

    override fun toJson(): JsonObject {
        return JsonObject(jacksonObjectMapper().writeValueAsString(this))
    }

    companion object {
        fun fromDonation(donation: Donation): DonationApiModel {
            return DonationApiModel(
                donation.id,
                donation.timestamp,
                donation.name,
                donation.message,
                donation.amount,
                donation.read,
                donation.externalId,
                listOf()
            )
        }

        fun fromDonationWithCodes(donationWithCodes: DonationWithCodes): DonationApiModel {
            val donation = donationWithCodes.donation
            val codes = donationWithCodes.incentives
            return DonationApiModel(
                donation.id,
                donation.timestamp,
                donation.name,
                donation.message,
                donation.amount,
                donation.read,
                donation.externalId,
                codes.map { generatedIncentive ->
                    IncentiveInDonationApi(
                        generatedIncentive.generatedCode.code,
                        generatedIncentive.chosenIncentives.map {
                            ChosenIncentiveInDonationApi(
                                it.incentiveId,
                                it.parameter
                            )
                        }
                    )
                }
            )
        }
    }
}
