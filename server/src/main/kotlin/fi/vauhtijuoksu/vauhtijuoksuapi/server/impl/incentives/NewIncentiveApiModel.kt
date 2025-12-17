package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives

import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.IncentiveType
import java.time.OffsetDateTime
import java.util.UUID

data class NewIncentiveApiModel(
    @JsonProperty("game_id")
    val gameId: UUID?,
    val title: String,
    @JsonProperty("end_time")
    val endTime: OffsetDateTime?,
    val type: String,
    val info: String?,
    val milestones: List<Int>?,
    @JsonProperty("option_parameters")
    val optionParameters: List<String>?,
    @JsonProperty("open_char_limit")
    val openCharLimit: Int?,
) {
    fun toIncentive(id: UUID): Incentive =
        Incentive(
            id,
            gameId,
            title,
            endTime,
            IncentiveType.valueOf(type.uppercase()),
            info,
            milestones,
            optionParameters,
            openCharLimit,
        )
}
