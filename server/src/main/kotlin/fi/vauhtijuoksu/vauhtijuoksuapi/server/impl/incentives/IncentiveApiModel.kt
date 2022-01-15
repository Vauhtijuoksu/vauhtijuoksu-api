package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.IncentiveType
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiModel
import io.vertx.core.json.JsonObject
import java.time.OffsetDateTime
import java.util.UUID

data class IncentiveApiModel(
    val id: UUID,
    @JsonProperty("game_id")
    val gameId: UUID?,
    val title: String,
    @JsonProperty("end_time")
    val endTime: OffsetDateTime?,
    val type: String,
    val info: String?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val milestones: List<Int>?,
    @JsonProperty("option_parameters")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val optionParameters: List<String>?,
    @JsonProperty("open_char_limit")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val openCharLimit: Int?,
) : ApiModel<Incentive> {
    companion object {
        fun fromIncentive(incentive: Incentive): IncentiveApiModel {
            return IncentiveApiModel(
                incentive.id,
                incentive.gameId,
                incentive.title,
                incentive.endTime,
                incentive.type.name.lowercase(),
                incentive.info,
                incentive.milestones,
                incentive.optionParameters,
                incentive.openCharLimit,
            )
        }
    }

    override fun toModel(): Incentive {
        return Incentive(
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

    override fun toJson(): JsonObject {
        return JsonObject.mapFrom(this)
    }
}
