package fi.vauhtijuoksu.vauhtijuoksuapi.database.models

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import java.util.Date
import java.util.UUID

data class TimerDbModel(
    val id: UUID,
    @JsonProperty("start_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val startTime: Date?,
    @JsonProperty("end_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val endTime: Date?,
) {
    fun toTimer(): Timer {
        return Timer(
            id,
            startTime,
            endTime
        )
    }
    companion object {
        fun fromTimer(timer: Timer): TimerDbModel {
            return TimerDbModel(
                timer.id,
                timer.startTime,
                timer.endTime
            )
        }
    }
}
