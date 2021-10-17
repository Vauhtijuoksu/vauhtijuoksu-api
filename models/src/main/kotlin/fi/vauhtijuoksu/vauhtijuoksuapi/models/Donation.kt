package fi.vauhtijuoksu.vauhtijuoksuapi.models

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import java.util.Date
import java.util.UUID

data class Donation(
    override val id: UUID?,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val timestamp: Date?,
    val name: String?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val message: String?,
    val amount: Float?,
    val read: Boolean = false,
) : Model
