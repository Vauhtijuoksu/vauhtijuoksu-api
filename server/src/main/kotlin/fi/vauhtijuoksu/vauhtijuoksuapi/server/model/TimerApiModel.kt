package fi.vauhtijuoksu.vauhtijuoksuapi.server.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import java.util.Date
import java.util.UUID

data class TimerApiModel(
    @JsonProperty("start_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val startTime: Date?,
    @JsonProperty("end_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val endTime: Date?,
) {
    companion object {
        fun from(data: Timer): TimerApiModel {
            return TimerApiModel(
                data.startTime,
                data.endTime
            )
        }
    }
    fun to(data: TimerApiModel, id: UUID): Timer {
        return Timer(
            id,
            data.startTime,
            data.endTime
        )
    }
}
