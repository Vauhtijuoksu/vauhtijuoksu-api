package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import io.vertx.core.Future
import io.vertx.core.Future.future
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.templates.SqlTemplate
import mu.KotlinLogging
import javax.inject.Inject

class DonationDatabase
@Inject constructor(
    private val client: SqlClient,
    configuration: DatabaseConfiguration
) :
    AbstractDatabase<Donation>(
        client,
        configuration,
        "donations",
        "timestamp",
        { Donation::class.java }
    ),
    VauhtijuoksuDatabase<Donation> {
    private val logger = KotlinLogging.logger {}

    override fun add(record: Donation): Future<Donation> {
        return future { p ->
            SqlTemplate.forUpdate(
                client,
                "INSERT INTO donations " +
                    "(name, message, timestamp, amount, read, external_id) VALUES " +
                    "(#{name}, #{message}, #{timestamp}, #{amount}, #{read}, #{external_id})" +
                    "RETURNING *"
            )
                .mapFrom(Donation::class.java)
                .mapTo(Donation::class.java)
                .execute(record)
                .onFailure { t ->
                    logger.warn {
                        "Failed to insert $record because of ${t.message}"
                    }
                    p.fail(t)
                }
                .onSuccess { res ->
                    p.complete(res.iterator().next())
                    logger.debug { "Inserted donation $record" }
                }
        }
    }

    override fun update(record: Donation): Future<Donation?> {
        return future { p ->
            SqlTemplate.forUpdate(
                client,
                "UPDATE donations SET " +
                    "name = #{name}, " +
                    "message = #{message}, " +
                    "timestamp = #{timestamp}, " +
                    "amount = #{amount}, " +
                    "read = #{read} " +
                    "WHERE id = #{id} RETURNING *"
            )
                .mapFrom(Donation::class.java)
                .mapTo(Donation::class.java)
                .execute(record)
                .onFailure { t ->
                    logger.warn {
                        "Failed to update $record because of ${t.message}"
                    }
                    p.fail(t)
                }
                .onSuccess { res ->
                    if (res.iterator().hasNext()) {
                        logger.debug { "Updated donation into $res" }
                        p.complete(res.iterator().next())
                    } else {
                        logger.debug { "No donation with id ${record.id} found" }
                        p.complete()
                    }
                }
        }
    }
}
