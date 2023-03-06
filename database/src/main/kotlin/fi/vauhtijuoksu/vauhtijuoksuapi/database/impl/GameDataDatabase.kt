package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.database.models.GameDataDbModel
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import io.vertx.core.Future
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.templates.SqlTemplate
import mu.KotlinLogging
import javax.inject.Inject

internal class GameDataDatabase @Inject constructor(
    private val client: SqlClient,
    configuration: DatabaseConfiguration,
) : AbstractModelDatabase<GameData, GameDataDbModel>(
    client,
    configuration,
    "gamedata",
    "start_time",
    GameDataDbModel::toGameData,
    GameDataDbModel::class,
),
    VauhtijuoksuDatabase<GameData> {
    private val logger = KotlinLogging.logger {}

    @Suppress("MaxLineLength") // SQL is prettier without too many splits
    override fun add(record: GameData): Future<Unit> {
        return SqlTemplate.forUpdate(
            client,
            "INSERT INTO gamedata " +
                "(id, game, player, start_time, end_time, category, device, published, vod_link, img_filename, player_twitch, meta) VALUES " +
                "(#{id}, #{game}, #{player}, #{start_time}, #{end_time}, #{category}, #{device}, #{published}, #{vod_link}, #{img_filename}, #{player_twitch}, #{meta} ) ",
        )
            .mapFrom(GameDataDbModel::class.java)
            .execute(GameDataDbModel.fromGameData(record))
            .recover {
                throw ServerError("Failed to insert $record because of ${it.message}")
            }
            .map {
                logger.debug { "Inserted gamedata $record" }
                return@map
            }
    }

    override fun update(record: GameData): Future<Unit> {
        return SqlTemplate.forUpdate(
            client,
            "UPDATE gamedata SET " +
                "game = #{game}, " +
                "player = #{player}, " +
                "start_time = #{start_time}, " +
                "end_time = #{end_time}, " +
                "category = #{category}, " +
                "device = #{device}, " +
                "published = #{published}, " +
                "vod_link = #{vod_link}, " +
                "img_filename = #{img_filename}, " +
                "player_twitch = #{player_twitch}, " +
                "meta = #{meta} " +
                "WHERE id = #{id}",
        )
            .mapFrom(GameDataDbModel::class.java)
            .execute(GameDataDbModel.fromGameData(record))
            .recover {
                throw ServerError("Failed to update $record because of ${it.message}")
            }
            .expectOneChangedRow()
            .map {
                logger.debug { "Updated GameData into $it" }
            }
    }
}
