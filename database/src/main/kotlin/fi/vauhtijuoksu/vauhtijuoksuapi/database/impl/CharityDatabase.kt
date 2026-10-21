package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.database.models.CharityDbModel
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Charity
import io.vertx.core.Future
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.templates.SqlTemplate
import jakarta.inject.Inject
import mu.KotlinLogging

class CharityDatabase
@Inject constructor(
    private val client: SqlClient,
    configuration: DatabaseConfiguration,
) : AbstractModelDatabase<Charity, CharityDbModel>(
    client,
    configuration,
    "charities",
    "name",
    CharityDbModel::toCharity,
    CharityDbModel::class,
),
    VauhtijuoksuDatabase<Charity> {
    private val logger = KotlinLogging.logger {}

    override fun add(record: Charity): Future<Unit> {
        return SqlTemplate.forUpdate(
            client,
            "INSERT INTO charities (id, name, url, goal, img_filename) " +
                "VALUES (#{id}, #{name}, #{url}, #{goal}, #{img_filename})",
        )
            .mapFrom(CharityDbModel::class.java)
            .execute(CharityDbModel.fromCharity(record))
            .recover {
                throw ServerError("Failed to insert $record because of ${it.message}")
            }
            .expectOneChangedRow()
            .map {
                logger.debug { "Inserted charity $record" }
            }
    }

    override fun update(record: Charity): Future<Unit> {
        return SqlTemplate.forUpdate(
            client,
            "UPDATE charities SET name = #{name}, url = #{url}, goal = #{goal}, " +
                "img_filename = #{img_filename} WHERE id = #{id}",
        )
            .mapFrom(CharityDbModel::class.java)
            .execute(CharityDbModel.fromCharity(record))
            .recover {
                throw ServerError("Failed to update $record because of ${it.message}")
            }
            .expectOneChangedRow()
    }
}
