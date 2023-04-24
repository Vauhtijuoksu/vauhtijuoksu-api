package fi.vauhtijuoksu.vauhtijuoksuapi.server.model

import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import java.time.OffsetDateTime
import java.util.UUID

data class TimerApiModel(
    @JsonProperty("start_time")
    val startTime: OffsetDateTime?,
    @JsonProperty("end_time")
    val endTime: OffsetDateTime?,
    val indexcol: Int?,
) {
    companion object {
        fun from(data: Timer): TimerApiModel {
            return TimerApiModel(
                data.startTime,
                data.endTime,
                data.indexcol,
            )
        }
    }
    fun to(data: TimerApiModel, id: UUID): Timer {
        return Timer(
            id,
            data.startTime,
            data.endTime,
            data.indexcol,
        )
    }
}
