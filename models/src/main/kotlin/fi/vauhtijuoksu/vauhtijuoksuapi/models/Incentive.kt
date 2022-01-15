package fi.vauhtijuoksu.vauhtijuoksuapi.models

import java.time.OffsetDateTime
import java.util.UUID

enum class IncentiveType {
    MILESTONE,
    OPTION,
    OPEN,
}

data class Incentive(
    override val id: UUID,
    val gameId: UUID?,
    val title: String,
    val endTime: OffsetDateTime?,
    val type: IncentiveType,
    val info: String?,
    val milestones: List<Int>?,
    val optionParameters: List<String>?,
    val openCharLimit: Int?,
) : Model {
    init {
        when (type) {
            IncentiveType.MILESTONE -> {
                milestones ?: throw IllegalArgumentException("milestones is required on milestone incentives")

                optionParameters?.let {
                    throw IllegalArgumentException("optionParameters is not allowed on milestone incentives")
                }
                openCharLimit?.let {
                    throw IllegalArgumentException("openCharLimit is not allowed on milestone incentives")
                }
            }

            IncentiveType.OPTION -> {
                milestones?.let {
                    throw IllegalArgumentException("milestones is not allowed on option incentives")
                }

                optionParameters ?: throw IllegalArgumentException("optionParameters required on option incentives")

                openCharLimit?.let {
                    throw IllegalArgumentException("openCharLimit is not allowed on option incentives")
                }
            }

            IncentiveType.OPEN -> {
                milestones?.let {
                    throw IllegalArgumentException("milestones is not allowed on open incentives")
                }

                optionParameters?.let {
                    throw IllegalArgumentException("optionParameters is not allowed on open incentives")
                }

                openCharLimit ?: throw IllegalArgumentException("openCharLimit required on open incentives")
            }
        }

        for (field in listOf(this::title, this::info)) {
            if (field.get()?.isEmpty() == true) {
                throw IllegalArgumentException("$field can't be empty")
            }
        }
    }
}
