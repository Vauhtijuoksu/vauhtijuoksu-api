package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.database.models.IncentiveDbModel
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import io.vertx.core.Future
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.templates.SqlTemplate
import io.vertx.sqlclient.templates.TupleMapper
import jakarta.inject.Inject
import mu.KotlinLogging

internal class IncentiveDatabase
    @Inject
    constructor(
        private val client: SqlClient,
        configuration: DatabaseConfiguration,
    ) : AbstractModelDatabase<Incentive, IncentiveDbModel>(
            client,
            configuration,
            "incentives",
            "\"endTime\"",
            IncentiveDbModel::toIncentive,
            IncentiveDbModel::class,
        ),
        VauhtijuoksuDatabase<Incentive> {
        private val logger = KotlinLogging.logger {}

        override fun add(record: Incentive): Future<Unit> =
            SqlTemplate
                .forUpdate(
                    client,
                    """INSERT INTO incentives VALUES
                |(#{id}, #{gameId}, #{title}, #{endTime}, #{type}, 
                |#{info}, #{milestones}, #{optionParameters}, #{openCharLimit})
                |
                    """.trimMargin(),
                ).mapTo { row -> row.toJson().mapTo(IncentiveDbModel::class.java) }
                .mapFrom(
                    TupleMapper.mapper { incentive: IncentiveDbModel ->
                        mapOf<String, Any?>(
                            "id" to incentive.id,
                            "gameId" to incentive.gameId,
                            "title" to incentive.title,
                            "endTime" to incentive.endTime,
                            "type" to incentive.type,
                            "info" to incentive.info,
                            "milestones" to incentive.milestones?.toTypedArray(),
                            "optionParameters" to incentive.optionParameters?.toTypedArray(),
                            "openCharLimit" to incentive.openCharLimit,
                        )
                    },
                ).execute(IncentiveDbModel.fromIncentive(record))
                .recover {
                    throw ServerError("Failed to insert $record because of ${it.message}")
                }.map {
                    logger.debug { "Inserted incentive $record" }
                }

        override fun update(record: Incentive): Future<Unit> =
            SqlTemplate
                .forUpdate(
                    client,
                    """
            UPDATE incentives SET  
                    id = #{id},  
                    "gameId" = #{gameId},  
                    title = #{title},  
                    "endTime" = #{endTime},  
                    type = #{type},  
                    info = #{info},  
                    milestones = #{milestones},  
                    "optionParameters" = #{optionParameters},  
                    "openCharLimit" = #{openCharLimit}  
                    WHERE id = #{id}
                    """,
                ).mapFrom(
                    TupleMapper.mapper { incentive: IncentiveDbModel ->
                        mapOf<String, Any?>(
                            "id" to incentive.id,
                            "gameId" to incentive.gameId,
                            "title" to incentive.title,
                            "endTime" to incentive.endTime,
                            "type" to incentive.type,
                            "info" to incentive.info,
                            "milestones" to incentive.milestones?.toTypedArray(),
                            "optionParameters" to incentive.optionParameters?.toTypedArray(),
                            "openCharLimit" to incentive.openCharLimit,
                        )
                    },
                ).execute(IncentiveDbModel.fromIncentive(record))
                .recover {
                    throw ServerError("Failed to update $record because of ${it.message}")
                }.expectOneChangedRow()
                .map {
                    logger.debug { "Updated Incentive into $it" }
                }
    }
