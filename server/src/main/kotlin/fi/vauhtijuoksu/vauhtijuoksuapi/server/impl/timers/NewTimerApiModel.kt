package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.timers

import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import java.time.OffsetDateTime
import java.util.UUID

data class NewTimerApiModel(
    @JsonProperty("start_time")
    val startTime: OffsetDateTime?,
    @JsonProperty("end_time")
    val endTime: OffsetDateTime?,
    val name: String,
) {
    fun toTimer(id: UUID): Timer =
        Timer(
            id,
            startTime,
            endTime,
            name,
        )
}
