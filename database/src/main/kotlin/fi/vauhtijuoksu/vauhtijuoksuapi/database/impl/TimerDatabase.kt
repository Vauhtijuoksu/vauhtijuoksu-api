package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.database.models.TimerDbModel
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import io.vertx.core.Future
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.templates.SqlTemplate
import io.vertx.sqlclient.templates.TupleMapper
import mu.KotlinLogging
import javax.inject.Inject

class TimerDatabase
@Inject constructor(
    private val client: SqlClient,
    configuration: DatabaseConfiguration,
) : AbstractModelDatabase<Timer, TimerDbModel>(
    client,
    configuration,
    "timers",
    null,
    TimerDbModel::toTimer,
    TimerDbModel::class,
),
    VauhtijuoksuDatabase<Timer> {
    private val logger = KotlinLogging.logger {}

    override fun add(record: Timer): Future<Unit> {
        return SqlTemplate.forUpdate(
            client,
            "INSERT INTO timers VALUES (#{id}, #{start_time}, #{end_time}) ",
        )
            .mapFrom(
                TupleMapper.mapper { timer: TimerDbModel ->
                    mapOf<String, Any?>(
                        "id" to timer.id,
                        "start_time" to timer.startTime,
                        "end_time" to timer.endTime,
                    )
                },
            )
            .execute(TimerDbModel.fromTimer(record))
            .recover {
                throw ServerError("Failed to insert $record because of ${it.message}")
            }
            .map {
                logger.debug { "Inserted timer $record" }
            }
    }

    override fun update(record: Timer): Future<Unit> {
        return SqlTemplate.forUpdate(
            client,
            "UPDATE timers SET " +
                "id = #{id}, " +
                "start_time = #{start_time}, " +
                "end_time = #{end_time} " +
                "WHERE id = #{id}",
        )
            .mapFrom(
                TupleMapper.mapper { timer: TimerDbModel ->
                    mapOf<String, Any?>(
                        "id" to timer.id,
                        "start_time" to timer.startTime,
                        "end_time" to timer.endTime,
                    )
                },
            )
            .mapTo(TimerDbModel::class.java)
            .execute(TimerDbModel.fromTimer(record))
            .recover {
                throw ServerError("Failed to update $record because of ${it.message}")
            }
            .expectOneChangedRow()
            .map {
                logger.debug { "Updated Timer into $it" }
            }
    }
}
