package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.database.models.TimerDbModel
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import io.vertx.core.Future
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.templates.SqlTemplate
import mu.KotlinLogging
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class TimerDatabase
@Inject constructor(
    private val client: SqlClient,
    configuration: DatabaseConfiguration
) : AbstractModelDatabase<Timer, TimerDbModel>(
    client,
    configuration,
    "timers",
    null,
    { TimerDbModel -> TimerDbModel.toTimer() }
),
    VauhtijuoksuDatabase<Timer> {
    private val logger = KotlinLogging.logger {}
    val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    override fun <I, R> mapToFunction(template: SqlTemplate<I, R>): SqlTemplate<I, RowSet<TimerDbModel>> {
        return template.mapTo(TimerDbModel::class.java)
    }

    override fun add(record: Timer): Future<Timer> {
        return SqlTemplate.forUpdate(
            client,
            "INSERT INTO timers " +
                "(start_time, end_time) VALUES " +
                "(#{start_time}, #{end_time}) " +
                "RETURNING *"
        )
            .mapFrom(TimerDbModel::class.java)
            .mapTo(TimerDbModel::class.java)
            .execute(TimerDbModel.fromTimer(record))
            .recover {
                throw ServerError("Failed to insert $record because of ${it.message}")
            }
            .map {
                logger.debug { "Inserted timer $record" }
                return@map it.iterator().next().toTimer()
            }
    }

    override fun addAll(records: List<Timer>): Future<List<Timer>?> {
        fun insertStatement(data: List<Timer>): String {
            fun valuesStringForTimer(timer: Timer): String {
                val startTime = if (timer.startTime != null) "'${df.format(timer.startTime)}'" else null
                val endTime = if (timer.endTime != null) "'${df.format(timer.endTime)}'" else null
                return "('${timer.id}', $startTime, $endTime)"
            }

            var statement = "INSERT INTO timers VALUES "
            for (timer in data) {
                statement += "${valuesStringForTimer(timer)},"
            }
            statement = statement.trim(',')
            statement += """ ON CONFLICT (id) DO UPDATE 
                SET start_time = excluded.start_time, 
                end_time = excluded.end_time;"""
            return statement
        }

        return client.query(insertStatement(records))
            .execute()
            .recover {
                throw ServerError("Failed to insert $records because of ${it.message}")
            }
            .map {
                return@map it.toList().map { row -> toTimer(row) }
            }
    }

    private fun toTimer(row: Row): Timer {
        val startTime = if (row.getString("start_time") != null)
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(row.getString("start_time"))))
        else null
        val endTime = if (row.getString("end_time") != null)
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(row.getString("end_time"))))
        else null
        return Timer(
            UUID.fromString(row.getString("id")),
            startTime,
            endTime
        )
    }

    override fun update(record: Timer): Future<Timer?> {
        return SqlTemplate.forUpdate(
            client,
            "UPDATE timers SET " +
                "start_time = #{start_time}, " +
                "end_time = #{end_time} " +
                "WHERE id = #{id} RETURNING *"
        )
            .mapFrom(TimerDbModel::class.java)
            .mapTo(TimerDbModel::class.java)
            .execute(TimerDbModel.fromTimer(record))
            .recover {
                throw ServerError("Failed to update $record because of ${it.message}")
            }
            .map {
                if (it.iterator().hasNext()) {
                    logger.debug { "Updated Timer into $it" }
                    return@map it.iterator().next().toTimer()
                } else {
                    logger.debug { "No Timer with id ${record.id} found" }
                    return@map null
                }
            }
    }
}
