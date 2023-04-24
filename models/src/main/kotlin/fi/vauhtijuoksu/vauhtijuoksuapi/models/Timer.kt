package fi.vauhtijuoksu.vauhtijuoksuapi.models

import java.time.OffsetDateTime
import java.util.UUID

data class Timer(
    override val id: UUID,
    val startTime: OffsetDateTime?,
    val endTime: OffsetDateTime?,
    val indexcol: Int?,
) : Model
