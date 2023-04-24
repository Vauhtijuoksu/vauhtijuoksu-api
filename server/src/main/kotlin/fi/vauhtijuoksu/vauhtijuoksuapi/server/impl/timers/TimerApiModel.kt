package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.timers

import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiModel
import io.vertx.core.json.JsonObject
import java.time.OffsetDateTime
import java.util.UUID

data class TimerApiModel(
    val id: UUID,
    @JsonProperty("start_time")
    val startTime: OffsetDateTime?,
    @JsonProperty("end_time")
    val endTime: OffsetDateTime?,
    val name: String,
) : ApiModel<Timer> {
    companion object {
        fun from(data: Timer): TimerApiModel {
            return TimerApiModel(
                data.id,
                data.startTime,
                data.endTime,
                data.name,
            )
        }
    }

    fun to(data: TimerApiModel): Timer {
        return Timer(
            data.id,
            data.startTime,
            data.endTime,
            data.name,
        )
    }

    override fun toModel(): Timer {
        return to(this)
    }

    override fun toJson(): JsonObject {
        return JsonObject.mapFrom(this)
    }
}
