package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentivecodes

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.GeneratedIncentiveCodeDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.UserError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.ChosenIncentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GeneratedIncentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.IncentiveCode
import fi.vauhtijuoksu.vauhtijuoksuapi.models.IncentiveType
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import javax.inject.Inject

class IncentiveCodeService @Inject constructor(
    private val db: GeneratedIncentiveCodeDatabase,
    private val incentiveDatabase: VauhtijuoksuDatabase<Incentive>,
) {
    fun generateCode(chosenIncentives: List<ChosenIncentive>): Future<IncentiveCode> {
        if (chosenIncentives.isEmpty()) {
            return Future.failedFuture(UserError("No incentives chosen"))
        }
        return CompositeFuture.all(
            chosenIncentives.map { chosenIncentive ->
                incentiveDatabase.getById(chosenIncentive.incentiveId)
                    .map {
                        if (it == null) {
                            throw UserError("Unknown chosen incentive with id ${chosenIncentive.incentiveId}")
                        }
                        it
                    }
                    .map { incentive ->
                        validateChosenIncentive(chosenIncentive, incentive)
                    }
            },
        ).flatMap {
            val code = IncentiveCode.random()
            db.add(
                GeneratedIncentive(
                    code,
                    chosenIncentives,
                ),
            ).map { code }
        }
    }

    @Suppress("ThrowsCount") // Why not, let's fail on error in validation
    private fun validateChosenIncentive(chosenIncentive: ChosenIncentive, incentive: Incentive) {
        when (incentive.type) {
            IncentiveType.MILESTONE -> {
                if (chosenIncentive.parameter != null) {
                    throw UserError("Parameter not allowed on milestone incentives")
                }
            }
            IncentiveType.OPTION -> {
                if (chosenIncentive.parameter == null) {
                    throw UserError("Parameter is required on open incentive")
                }
                if (!incentive.optionParameters!!.contains(chosenIncentive.parameter)) {
                    throw UserError("Unknown parameter ${chosenIncentive.parameter}")
                }
            }
            IncentiveType.OPEN -> {
                if (chosenIncentive.parameter == null) {
                    throw UserError("Parameter is required on open incentive")
                }
                if (chosenIncentive.parameter!!.length > incentive.openCharLimit!!) {
                    throw UserError("Too long parameter $incentive")
                }
            }
        }
    }
}
