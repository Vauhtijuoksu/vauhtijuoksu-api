package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.database.models.GameDataDbModel
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.MissingEntityException
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import io.vertx.core.Future
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.Tuple
import io.vertx.sqlclient.templates.SqlTemplate
import java.util.UUID
import javax.inject.Inject
import kotlin.NoSuchElementException

internal class GameDataDatabase
@Inject constructor(
    private val pool: PgPool,
    configuration: DatabaseConfiguration,
) : BaseDatabase(configuration),
    VauhtijuoksuDatabase<GameData> {
    private val selectClause =
        """SELECT gamedata.*, 
    |array_remove(array_agg(players_in_game.player_id ORDER BY players_in_game.player_order ASC), NULL) as player_ids 
    |FROM gamedata LEFT JOIN players_in_game ON gamedata.id = players_in_game.game_id 
    |GROUP BY gamedata.id 
    |ORDER BY start_time ASC
        """.trimMargin()
    private val getAllQuery = pool.preparedQuery(selectClause)

    private val getByIdQuery =
        pool.preparedQuery(
            """SELECT gamedata.*,
    | array_remove(array_agg(players_in_game.player_id ORDER BY players_in_game.player_order ASC), NULL) as player_ids 
    |FROM gamedata LEFT JOIN players_in_game ON gamedata.id = players_in_game.game_id 
    |WHERE gamedata.id=$1 
    |GROUP BY gamedata.id
            """.trimMargin(),
        )
    private val deleteQuery = pool.preparedQuery("DELETE FROM gamedata WHERE id = $1")

    @Suppress("MaxLineLength") // SQL is prettier without too many splits
    override fun add(record: GameData): Future<Unit> {
        return pool.withTransaction { client ->
            SqlTemplate
                .forUpdate(
                    client,
                    """INSERT INTO gamedata 
                            (id, game, start_time, end_time, category, device, published, vod_link, img_filename, meta) VALUES 
                            (#{id}, #{game}, #{start_time}, #{end_time}, #{category}, #{device}, #{published}, #{vod_link}, #{img_filename}, #{meta} )""",
                )
                .mapFrom(GameDataDbModel::class.java)
                .execute(GameDataDbModel.fromGameData(record))
                .flatMap {
                    if (record.players.isNotEmpty()) {
                        client
                            .preparedQuery("""INSERT INTO players_in_game (game_id, player_id, player_order) VALUES ($1, $2, $3)""")
                            .executeBatch(record.players.mapIndexed { i, playerId -> Tuple.of(record.id, playerId, i) })
                    } else {
                        Future.succeededFuture()
                    }
                }
                .recover {
                    throw ServerError("Failed to insert $record because of ${it.message}")
                }
                .mapEmpty()
        }
    }

    override fun update(record: GameData): Future<Unit> {
        return pool.withTransaction { client ->
            SqlTemplate
                .forUpdate(
                    pool,
                    "UPDATE gamedata SET " +
                        "game = #{game}, " +
                        "start_time = #{start_time}, " +
                        "end_time = #{end_time}, " +
                        "category = #{category}, " +
                        "device = #{device}, " +
                        "published = #{published}, " +
                        "vod_link = #{vod_link}, " +
                        "img_filename = #{img_filename}, " +
                        "meta = #{meta} " +
                        "WHERE id = #{id}",
                )
                .mapFrom(GameDataDbModel::class.java)
                .execute(GameDataDbModel.fromGameData(record))
                .expectOneChangedRow()
                .flatMap {
                    client.preparedQuery("DELETE FROM players_in_game WHERE game_id=$1")
                        .execute(Tuple.of(record.id))
                }
                .flatMap {
                    if (record.players.isNotEmpty()) {
                        client.preparedQuery(
                            """INSERT INTO players_in_game (game_id, player_id, player_order) VALUES ($1, $2, $3)""",
                        )
                            .executeBatch(record.players.mapIndexed { i, playerId -> Tuple.of(record.id, playerId, i) })
                    } else {
                        Future.succeededFuture()
                    }
                }
                .orServerError()
                .mapEmpty()
        }
    }

    override fun getAll(): Future<List<GameData>> {
        return getAllQuery.execute()
            .map { it.map(mapperToType<GameDataDbModel>()) }
            .map { it.map(GameDataDbModel::toGameData) }
            .orServerError()
    }

    override fun getById(id: UUID): Future<GameData> {
        return getByIdQuery.execute(Tuple.of(id))
            .map {
                return@map it.first()
            }.recover {
                if (it is NoSuchElementException) {
                    throw MissingEntityException("No GameData with id $id")
                } else {
                    throw it
                }
            }
            .map(mapperToType<GameDataDbModel>())
            .map(GameDataDbModel::toGameData)
    }

    override fun delete(id: UUID): Future<Unit> {
        return deleteQuery.execute(Tuple.of(id))
            .expectOneChangedRow()
    }
}
