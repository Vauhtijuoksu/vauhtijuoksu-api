package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.fasterxml.jackson.module.kotlin.kotlinModule
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import io.vertx.core.Future
import io.vertx.core.Future.future
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.sqlclient.PreparedQuery
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple
import io.vertx.sqlclient.templates.SqlTemplate
import mu.KotlinLogging
import org.flywaydb.core.Flyway
import java.util.Collections
import java.util.UUID
import javax.inject.Inject

class VauhtijuoksuDatabaseImpl @Inject constructor(
    private val client: SqlClient,
    configuration: DatabaseConfiguration
) : VauhtijuoksuDatabase {
    private val logger = KotlinLogging.logger {}

    private val deleteQuery: PreparedQuery<RowSet<Row>>

    init {
        val migrations = Flyway.configure().dataSource(
            "jdbc:postgresql://${configuration.address}:${configuration.port}/${configuration.database}?sslmode=prefer",
            configuration.user,
            configuration.password
        ).load()
        migrations.migrate()

        DatabindCodec.mapper().registerModule(kotlinModule())

        DatabindCodec.prettyMapper().registerModule(kotlinModule())

        deleteQuery = client.preparedQuery("DELETE FROM gamedata WHERE id = $1")
    }

    override fun getGameData(): Future<List<GameData>> {
        logger.debug { "Get all gamedata objects" }
        return future { p ->
            SqlTemplate.forQuery(client, "SELECT * FROM gamedata ORDER BY start_time ASC")
                .mapTo(GameData::class.java)
                .execute(Collections.emptyMap())
                .onFailure { t ->
                    logger.warn { "Failed to retrieve gamedata because ${t.message}" }
                    p.fail(t)
                }
                .onSuccess { gameData ->
                    p.complete(gameData.toList())
                    logger.debug { "All of gamedata objects returned" }
                }
        }
    }

    override fun getGameDataById(id: UUID): Future<GameData?> {
        logger.debug { "Get gamedata by id $id" }
        return future { p ->
            SqlTemplate.forQuery(client, "SELECT * FROM gamedata WHERE id = #{id}")
                .mapTo(GameData::class.java)
                .execute(Collections.singletonMap("id", id) as Map<String, Any>?)
                .onFailure { t ->
                    logger.warn { "Failed to retrieve gamedata because ${t.message}" }
                    p.fail(t)
                }
                .onSuccess { gamedataRows ->
                    if (gamedataRows.iterator().hasNext()) {
                        p.complete(gamedataRows.iterator().next())
                        logger.debug { "Found gamedata with id $id" }
                    } else {
                        p.complete()
                        logger.debug { "No gamedata found by id $id" }
                    }
                }
        }
    }

    @Suppress("MaxLineLength") // SQL is prettier without too many splits
    override fun addGameData(gd: GameData): Future<GameData> {
        return future { p ->
            SqlTemplate.forUpdate(
                client,
                "INSERT INTO gamedata " +
                    "(game, player, start_time, end_time, category, device, published, vod_link, img_filename, player_twitch) VALUES " +
                    "(#{game}, #{player}, #{start_time}, #{end_time}, #{category}, #{device}, #{published}, #{vod_link}, #{img_filename}, #{player_twitch} ) " +
                    "RETURNING *"
            )
                .mapFrom(GameData::class.java)
                .mapTo(GameData::class.java)
                .execute(gd)
                .onFailure { t ->
                    logger.warn {
                        "Failed to insert $gd because of ${t.message}"
                    }
                    p.fail(t)
                }
                .onSuccess { res ->
                    p.complete(res.iterator().next())
                    logger.debug { "Inserted gamedata $gd" }
                }
        }
    }

    override fun deleteGameData(id: UUID): Future<Boolean> {
        return future { p ->
            deleteQuery
                .execute(Tuple.of(id))
                .onFailure { t ->
                    logger.warn { "Failed to delete gamedata with id $id because ${t.message}" }
                    p.fail(t)
                }
                .onSuccess { res ->
                    if (res.rowCount() == 1) {
                        p.complete(true)
                    } else {
                        p.complete(false)
                    }
                }
        }
    }
}
