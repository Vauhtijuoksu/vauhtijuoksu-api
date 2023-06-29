package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.SingletonDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.database.models.PlayerInfoDbModel
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.PlayerInfo
import io.vertx.core.Future
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple
import io.vertx.sqlclient.templates.SqlTemplate
import jakarta.inject.Inject
import java.util.Collections

internal class PlayerInfoDatabase
@Inject constructor(
    configuration: DatabaseConfiguration,
    private val client: SqlClient,
) :
    BaseDatabase(configuration),
    SingletonDatabase<PlayerInfo> {

    override fun get(): Future<PlayerInfo> {
        return SqlTemplate.forQuery(client, "SELECT * FROM player_info LIMIT 1")
            .mapTo(PlayerInfoDbModel::class.java)
            .execute(Collections.emptyMap())
            .recover {
                throw ServerError("Failed to get player info: ${it.message}")
            }
            .map {
                return@map it.first().toPlayerInfo()
            }
    }

    override fun save(record: PlayerInfo): Future<Void> {
        return client.preparedQuery(
            """UPDATE player_info SET 
                        message = $1
                        """,
        ).execute(
            Tuple.of(
                record.message,
            ),
        ).recover {
            throw ServerError(it)
        }.mapEmpty()
    }
}
