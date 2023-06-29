package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.database.models.PlayerDbModel
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.MissingEntityException
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Player
import io.vertx.core.Future
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.templates.SqlTemplate
import jakarta.inject.Inject
import mu.KotlinLogging

class PlayerDatabase
@Inject constructor(
    private val client: SqlClient,
    configuration: DatabaseConfiguration,
) :
    AbstractModelDatabase<Player, PlayerDbModel>(
        client,
        configuration,
        "players",
        "display_name",
        PlayerDbModel::toPlayer,
        PlayerDbModel::class,
    ),
    VauhtijuoksuDatabase<Player> {
    private val logger = KotlinLogging.logger {}

    override fun add(record: Player): Future<Unit> {
        return SqlTemplate.forUpdate(
            client,
            """INSERT INTO players VALUES (#{id}, #{display_name}, #{twitch_channel}, #{discord_nick})""",
        )
            .mapFrom(PlayerDbModel::class.java)
            .execute(PlayerDbModel.fromPlayer(record))
            .recover {
                throw ServerError("Failed to insert $record because of ${it.message}")
            }
            .onSuccess {
                logger.debug { "Inserted player $record" }
            }
            .mapEmpty()
    }

    override fun update(record: Player): Future<Unit> {
        return SqlTemplate.forUpdate(
            client,
            """UPDATE players SET  
                    display_name = #{display_name},
                    twitch_channel = #{twitch_channel},
                    discord_nick = #{discord_nick}
                    WHERE id = #{id}""",
        )
            .mapFrom(PlayerDbModel::class.java)
            .execute(PlayerDbModel.fromPlayer(record))
            .recover {
                throw ServerError("Failed to update $record because of ${it.message}")
            }
            .map {
                if (it.rowCount() == 1) {
                    logger.debug { "Updated Player into $it" }
                    return@map
                } else {
                    logger.debug { "No Player with id ${record.id} found" }
                    throw MissingEntityException("No Player with id ${record.id}")
                }
            }
    }
}
