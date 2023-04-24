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
    val indexcol: Int?,
) {
    fun toTimer(): Timer {
        return Timer(
            id,
            startTime,
            endTime,
            indexcol,
        )
    }
    companion object {
        fun fromTimer(timer: Timer): TimerDbModel {
            return TimerDbModel(
                timer.id,
                timer.startTime,
                timer.endTime,
                timer.indexcol,
            )
        }
    }
}
