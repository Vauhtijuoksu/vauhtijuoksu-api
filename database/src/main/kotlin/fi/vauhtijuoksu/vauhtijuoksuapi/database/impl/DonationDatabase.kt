package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import io.vertx.core.Future
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
        return Future.future { p ->
            SqlTemplate.forUpdate(
                client,
                "INSERT INTO donations " +
                    "(name, message, timestamp, amount, read) VALUES " +
                    "(#{name}, #{message}, #{timestamp}, #{amount}, #{read})" +
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
                    logger.debug { "Inserted gamedata $record" }
                }
        }
    }
}
