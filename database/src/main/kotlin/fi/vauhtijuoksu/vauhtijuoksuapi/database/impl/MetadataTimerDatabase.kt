package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.database.models.StreamMetadataDbModel
import fi.vauhtijuoksu.vauhtijuoksuapi.database.models.TimerDbModel
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.StreamMetadata
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import io.vertx.core.Future
import io.vertx.core.Future.future
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple
import io.vertx.sqlclient.templates.SqlTemplate
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.UUID
import javax.inject.Inject

open class MetadataTimerDatabase
@Inject constructor(
    configuration: DatabaseConfiguration,
    private val client: SqlClient,
) :
    BaseDatabase(configuration) {

    open fun get(): Future<StreamMetadata> {
        return SqlTemplate.forQuery(client, "SELECT * FROM stream_metadata LIMIT 1")
            .mapTo(StreamMetadataDbModel::class.java)
            .execute(Collections.emptyMap())
            .recover {
                throw ServerError("Failed to get stream metadata: ${it.message}")
            }
            .map {
                return@map it.first().toStreamMetadata()
            }
            .compose {
                var results = it
                SqlTemplate.forQuery(client, "SELECT * FROM timers")
                    .mapTo(TimerDbModel::class.java)
                    .execute(Collections.emptyMap())
                    .recover {
                        throw ServerError("Failed to retrieve records because ${it.message}")
                    }.map {
                        results.timers = it.toList().map({ TimerDbModel -> TimerDbModel.toTimer() })
                        return@map results
                    }
            }
    }

    open fun save(record: StreamMetadata): Future<Void> {
        return client.preparedQuery(
            """UPDATE stream_metadata SET 
                        donation_goal = $1,
                        current_game_id = $2,
                        donatebar_info = $3,
                        counters = $4,
                        heart_rates = $5,
                        now_playing = $6
                        """,
        ).execute(
            Tuple.of(
                record.donationGoal,
                record.currentGameId,
                record.donateBarInfo.toTypedArray(),
                record.counters.toTypedArray(),
                record.heartRates.toTypedArray(),
                record.nowPlaying,
            ),
        ).recover {
            throw ServerError(it)
        }
            .compose {
                if (record.timers.isNotEmpty()) {
                    addAllTimers(record.timers)
                } else {
                    future { p ->
                        p.complete()
                    }
                }
            }
            .mapEmpty()
    }

    open fun addAllTimers(records: List<Timer>): Future<List<Timer>?> {
        fun insertStatement(data: List<Timer>): String {
            fun valuesStringForTimer(timer: Timer): String {
                val startTime = if (timer.startTime != null) "'${timer.startTime}'" else null
                val endTime = if (timer.endTime != null) "'${timer.endTime}'" else null
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
        val startTime = if (row.getString("start_time") != null) {
            OffsetDateTime.ofInstant(
                Instant.from(DateTimeFormatter.ISO_INSTANT.parse(row.getString("start_time"))),
                ZoneId.of("Z"),
            )
        } else {
            null
        }
        val endTime = if (row.getString("end_time") != null) {
            OffsetDateTime.ofInstant(
                Instant.from(DateTimeFormatter.ISO_INSTANT.parse(row.getString("end_time"))),
                ZoneId.of("Z"),
            )
        } else {
            null
        }
        return Timer(
            UUID.fromString(row.getString("id")),
            startTime,
            endTime,
        )
    }
}
