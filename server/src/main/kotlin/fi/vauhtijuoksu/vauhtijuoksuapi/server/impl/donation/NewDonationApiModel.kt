package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.donation

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import java.util.Date
import java.util.UUID

data class NewDonationApiModel(
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val timestamp: Date,
    val name: String,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val message: String?,
    @JsonSetter(nulls = Nulls.FAIL)
    val amount: Float,
    val read: Boolean = false,
    @JsonProperty("external_id")
    val externalId: String?,
) {
    fun toDonation(id: UUID): Donation {
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
}
