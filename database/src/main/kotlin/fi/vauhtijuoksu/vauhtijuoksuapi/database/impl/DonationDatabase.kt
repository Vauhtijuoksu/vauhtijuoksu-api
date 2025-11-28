package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.database.models.DonationDbModel
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import io.vertx.core.Future
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.templates.SqlTemplate
import jakarta.inject.Inject
import mu.KotlinLogging

class DonationDatabase
    @Inject
    constructor(
        private val client: SqlClient,
        configuration: DatabaseConfiguration,
    ) : AbstractModelDatabase<Donation, DonationDbModel>(
            client,
            configuration,
            "donations",
            "timestamp",
            { donationDbModel -> donationDbModel.toDonation() },
            DonationDbModel::class,
        ),
        VauhtijuoksuDatabase<Donation> {
        private val logger = KotlinLogging.logger {}

        override fun add(record: Donation): Future<Unit> =
            SqlTemplate
                .forUpdate(
                    client,
                    "INSERT INTO donations " +
                        "(id, name, message, timestamp, amount, read, external_id) VALUES " +
                        "(#{id}, #{name}, #{message}, #{timestamp}, #{amount}, #{read}, #{external_id})",
                ).mapFrom(DonationDbModel::class.java)
                .execute(DonationDbModel.fromDonation(record))
                .recover {
                    throw ServerError("Failed to insert $record because of ${it.message}")
                }.expectOneChangedRow()
                .map {
                    logger.debug { "Inserted donation $record" }
                }

        override fun update(record: Donation): Future<Unit> =
            SqlTemplate
                .forUpdate(
                    client,
                    "UPDATE donations SET " +
                        "name = #{name}, " +
                        "message = #{message}, " +
                        "timestamp = #{timestamp}, " +
                        "amount = #{amount}, " +
                        "read = #{read} " +
                        "WHERE id = #{id}",
                ).mapFrom(DonationDbModel::class.java)
                .execute(DonationDbModel.fromDonation(record))
                .recover {
                    throw ServerError("Failed to update $record because of ${it.message}")
                }.expectOneChangedRow()
    }
