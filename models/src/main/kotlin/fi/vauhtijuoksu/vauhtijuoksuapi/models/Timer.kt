package fi.vauhtijuoksu.vauhtijuoksuapi.models

import java.util.Date
import java.util.UUID

data class Timer(
    override val id: UUID,
    val startTime: Date?,
    val endTime: Date?
) : Model
