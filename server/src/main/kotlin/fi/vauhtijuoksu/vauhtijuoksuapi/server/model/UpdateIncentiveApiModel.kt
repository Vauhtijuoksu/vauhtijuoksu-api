package fi.vauhtijuoksu.vauhtijuoksuapi.server.model

import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.IncentiveType
import java.util.Date
import java.util.UUID

data class UpdateIncentiveApiModel(
    @JsonProperty("game_id")
    val gameId: UUID?,
    val title: String?,
    @JsonProperty("end_time")
    val endTime: Date?,
    val type: IncentiveType?,
    val info: String?,
    val milestones: List<Int>?,
    @JsonProperty("option_parameters")
    val optionParameters: List<String>?,
    @JsonProperty("open_char_limit")
    val openCharLimit: Int?,
)
