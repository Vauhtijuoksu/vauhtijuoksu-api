package fi.vauhtijuoksu.vauhtijuoksuapi.database.models

import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import java.time.OffsetDateTime
import java.util.UUID

data class TimerDbModel(
    val id: UUID,
    @JsonProperty("start_time")
    val startTime: OffsetDateTime?,
    @JsonProperty("end_time")
    val endTime: OffsetDateTime?,
    val name: String,
) {
    fun toTimer(): Timer =
        Timer(
            id,
            startTime,
            endTime,
            name,
        )

    companion object {
        fun fromTimer(timer: Timer): TimerDbModel =
            TimerDbModel(
                timer.id,
                timer.startTime,
                timer.endTime,
                timer.name,
            )
    }
}
