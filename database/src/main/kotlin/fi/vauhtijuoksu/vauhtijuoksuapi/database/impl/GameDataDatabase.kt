package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import io.vertx.core.Future
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.templates.SqlTemplate
import mu.KotlinLogging
import javax.inject.Inject

class GameDataDatabase @Inject constructor(
    private val client: SqlClient,
    configuration: DatabaseConfiguration
) : AbstractDatabase<GameData>(
    client,
    configuration,
    "gamedata",
    "start_time",
    { GameData::class.java }
),
    VauhtijuoksuDatabase<GameData> {
    private val logger = KotlinLogging.logger {}

    @Suppress("MaxLineLength") // SQL is prettier without too many splits
    override fun add(record: GameData): Future<GameData> {
        return Future.future { p ->
            SqlTemplate.forUpdate(
                client,
                "INSERT INTO gamedata " +
                    "(game, player, start_time, end_time, category, device, published, vod_link, img_filename, player_twitch) VALUES " +
                    "(#{game}, #{player}, #{start_time}, #{end_time}, #{category}, #{device}, #{published}, #{vod_link}, #{img_filename}, #{player_twitch} ) " +
                    "RETURNING *"
            )
                .mapFrom(GameData::class.java)
                .mapTo(GameData::class.java)
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
