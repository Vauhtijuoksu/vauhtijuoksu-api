package fi.vauhtijuoksu.vauhtijuoksuapi.database.models

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import java.util.Date
import java.util.UUID

data class DonationDbModel(
    val id: UUID,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val timestamp: Date,
    val name: String,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val message: String?,
    val amount: Float,
    val read: Boolean,
    @JsonProperty("external_id")
    val externalId: String?,
) {
    fun toDonation(): Donation {
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

    companion object {
        fun fromDonation(donation: Donation): DonationDbModel {
            return DonationDbModel(
                donation.id,
                donation.timestamp,
                donation.name,
                donation.message,
                donation.amount,
                donation.read,
                donation.externalId,
            )
        }
    }
}
