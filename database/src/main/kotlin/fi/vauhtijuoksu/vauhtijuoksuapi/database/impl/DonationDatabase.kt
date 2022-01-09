package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
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
    AbstractModelDatabase<Donation>(
        client,
        configuration,
        "donations",
        "timestamp",
        { Donation::class.java }
    ),
    VauhtijuoksuDatabase<Donation> {
    private val logger = KotlinLogging.logger {}

    override fun add(record: Donation): Future<Donation> {
        return SqlTemplate.forUpdate(
            client,
            "INSERT INTO donations " +
                "(name, message, timestamp, amount, read, external_id) VALUES " +
                "(#{name}, #{message}, #{timestamp}, #{amount}, #{read}, #{external_id})" +
                "RETURNING *"
        )
            .mapFrom(Donation::class.java)
            .mapTo(Donation::class.java)
            .execute(record)
            .recover {
                throw ServerError("Failed to insert $record because of ${it.message}")
            }
            .map {
                logger.debug { "Inserted donation $record" }
                return@map it.iterator().next()
            }
    }

    override fun update(record: Donation): Future<Donation?> {
        return SqlTemplate.forUpdate(
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
            .recover {
                throw ServerError("Failed to update $record because of ${it.message}")
            }
            .map {
                if (it.iterator().hasNext()) {
                    logger.debug { "Updated donation into $it" }
                    return@map it.iterator().next()
                } else {
                    logger.debug { "No donation with id ${record.id} found" }
                    return@map null
                }
            }
    }
}
