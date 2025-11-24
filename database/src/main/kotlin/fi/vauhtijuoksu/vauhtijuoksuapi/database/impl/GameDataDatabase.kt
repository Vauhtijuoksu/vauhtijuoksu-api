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
import jakarta.inject.Inject
import mu.KotlinLogging
import java.util.UUID

internal class GameDataDatabase
@Inject constructor(
    private val pool: PgPool,
    configuration: DatabaseConfiguration,
) : BaseDatabase(configuration),
    VauhtijuoksuDatabase<GameData> {

    private val logger = KotlinLogging.logger {}

    private val selectClause =
        """
        SELECT
        gamedata.*,
        COALESCE(
            (
                SELECT json_agg(participant_in_game ORDER BY participant_order)
                FROM participant_in_game
                WHERE participant_in_game.game_id = gamedata.id
            ),
            '[]'
        ) AS participants
        FROM gamedata
        ORDER BY gamedata.start_time ASC;
        """
    private val getAllQuery = pool.preparedQuery(selectClause)

    private val getByIdQuery =
        pool.preparedQuery(
            """
            SELECT gamedata.*,
            COALESCE(json_agg(participant_in_game ORDER BY participant_order) FILTER (WHERE participant_in_game.game_id IS NOT NULL), '[]') AS participants
            FROM gamedata 
            LEFT JOIN participant_in_game ON gamedata.id = participant_in_game.game_id
            WHERE gamedata.id=$1 
            GROUP BY gamedata.id
            """,
        )
    private val deleteQuery = pool.preparedQuery("DELETE FROM gamedata WHERE id = $1")

    private val insertParticipant =
        """
        INSERT INTO participant_in_game 
        (game_id, participant_id, role_in_game, participant_order) VALUES 
        ($1, $2, $3, $4)
        """

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
                    if (record.participants.isNotEmpty()) {
                        client
                            .preparedQuery("""INSERT INTO participant_in_game (game_id, participant_id, role_in_game, participant_order) VALUES ($1, $2, $3, $4)""")
                            .executeBatch(
                                record.participants.mapIndexed { i, p ->
                                    Tuple.of(
                                        record.id,
                                        p.participantId,
                                        p.role,
                                        i,
                                    )
                                },
                            )
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
        logger.debug { "Updating $record" }
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
                    client.preparedQuery("DELETE FROM participant_in_game WHERE game_id=$1")
                        .execute(Tuple.of(record.id))
                }
                .flatMap {
                    if (record.participants.isNotEmpty()) {
                        client.preparedQuery(insertParticipant)
                            .executeBatch(
                                record.participants.mapIndexed { i, p ->
                                    Tuple.of(
                                        record.id,
                                        p.participantId,
                                        p.role.name,
                                        i,
                                    )
                                },
                            )
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
        logger.debug { "Getting game data by id $id" }
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
            .map {
                mapperToType<GameDataDbModel>()(it)
            }
            .map {
                it.toGameData()
            }
    }

    override fun delete(id: UUID): Future<Unit> {
        return deleteQuery.execute(Tuple.of(id))
            .expectOneChangedRow()
    }
}
