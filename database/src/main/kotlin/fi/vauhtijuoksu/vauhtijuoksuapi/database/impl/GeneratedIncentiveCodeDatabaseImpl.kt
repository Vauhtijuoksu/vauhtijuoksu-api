package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.GeneratedIncentiveCodeDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.ChosenIncentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GeneratedIncentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.IncentiveCode
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Tuple
import javax.inject.Inject

internal class GeneratedIncentiveCodeDatabaseImpl
@Inject constructor(
    configuration: DatabaseConfiguration,
    private val client: Pool,
) : GeneratedIncentiveCodeDatabase, BaseDatabase(configuration) {

    override fun getAll(): Future<List<GeneratedIncentive>> {
        data class CodeWithChosenIncentive(
            val code: IncentiveCode,
            val chosenIncentive: ChosenIncentive,
        )
        return client.preparedQuery(
            "SELECT * FROM incentive_codes " +
                "INNER JOIN chosen_incentives ON incentive_codes.id = chosen_incentives.incentive_code",
        )
            .mapping {
                CodeWithChosenIncentive(
                    IncentiveCode(it.getString("id")),
                    ChosenIncentive(
                        it.getUUID("incentive_id"),
                        it.getString("parameter"),
                    ),
                )
            }
            .execute()
            .onFailure { cause -> throw ServerError(cause) }
            .map { rows ->
                rows.toList()
                    .groupBy { it.code }
                    .map { entry ->
                        GeneratedIncentive(
                            entry.key,
                            entry.value.map { it.chosenIncentive },
                        )
                    }
            }
    }

    override fun add(record: GeneratedIncentive): Future<Void> {
        return client.withTransaction {
            CompositeFuture.all(
                it.preparedQuery("INSERT INTO incentive_codes values ($1)")
                    .execute(Tuple.of(record.generatedCode.code)),
                it.preparedQuery("INSERT INTO chosen_incentives values ($1, $2, $3)")
                    .executeBatch(
                        record.chosenIncentives.map { chosenIncentive ->
                            Tuple.of(
                                chosenIncentive.incentiveId,
                                record.generatedCode.code,
                                chosenIncentive.parameter,
                            )
                        },
                    ),
            )
        }.recover {
            throw ServerError(it)
        }.mapEmpty()
    }
}
