package fi.vauhtijuoksu.vauhtijuoksuapi.models

import java.util.Date
import java.util.UUID

data class Donation(
    override val id: UUID,
    val timestamp: Date,
    val name: String,
    val message: String?,
    val amount: Float,
    val read: Boolean = false,
    val externalId: String?,
) : Model
