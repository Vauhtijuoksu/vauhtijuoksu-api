package fi.vauhtijuoksu.vauhtijuoksuapi.database.models

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.IncentiveType
import java.time.OffsetDateTime
import java.util.UUID

data class IncentiveDbModel(
    val id: UUID,
    val gameId: UUID?,
    val title: String,
    val endTime: OffsetDateTime?,
    val type: IncentiveType,
    val info: String?,
    val milestones: List<Int>?,
    val optionParameters: List<String>?,
    val openCharLimit: Int?,
) {
    fun toIncentive(): Incentive {
        return Incentive(
            id,
            gameId,
            title,
            endTime,
            type,
            info,
            milestones,
            optionParameters,
            openCharLimit,
        )
    }

    companion object {
        fun fromIncentive(incentive: Incentive): IncentiveDbModel {
            return IncentiveDbModel(
                incentive.id,
                incentive.gameId,
                incentive.title,
                incentive.endTime,
                incentive.type,
                incentive.info,
                incentive.milestones,
                incentive.optionParameters,
                incentive.openCharLimit,
            )
        }
    }
}
