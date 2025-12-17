package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.IncentiveType
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiModel
import io.vertx.core.json.JsonObject
import java.time.OffsetDateTime
import java.util.UUID

data class StatusModel(
    val type: String,
    @JsonProperty("milestone_goal")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val milestoneGoal: Int?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val status: MilestoneStatus?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val option: String?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val amount: Double?,
)

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
    @JsonProperty("total_amount")
    val totalAmount: Double,
    val status: List<StatusModel>,
) : ApiModel<Incentive> {
    companion object {
        fun fromIncentive(incentive: Incentive): IncentiveApiModel =
            IncentiveApiModel(
                incentive.id,
                incentive.gameId,
                incentive.title,
                incentive.endTime,
                incentive.type.name.lowercase(),
                incentive.info,
                incentive.milestones,
                incentive.optionParameters,
                incentive.openCharLimit,
                0.0,
                listOf(),
            )

        fun fromIncentiveWithStatuses(incentiveWithStatuses: IncentiveWithStatuses): IncentiveApiModel {
            val incentive = incentiveWithStatuses.incentive
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
                incentiveWithStatuses.total,
                incentiveWithStatuses.statuses.map {
                    StatusModel(
                        it.type,
                        if (it is MilestoneIncentiveStatus) it.milestoneGoal else null,
                        if (it is MilestoneIncentiveStatus) it.status else null,
                        when (it) {
                            is OptionIncentiveStatus -> it.option
                            else -> null
                        },
                        when (it) {
                            is OptionIncentiveStatus -> it.amount
                            else -> null
                        },
                    )
                },
            )
        }
    }

    override fun toModel(): Incentive =
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

    override fun toJson(): JsonObject = JsonObject.mapFrom(this)
}
