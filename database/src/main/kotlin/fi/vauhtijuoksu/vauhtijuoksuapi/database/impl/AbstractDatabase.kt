package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.fasterxml.jackson.module.kotlin.kotlinModule
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model
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

/**
 * Usable for data that uses an uuid field named id as a primary key
 */
abstract class AbstractDatabase<T : Model>(
    private val client: SqlClient,
    configuration: DatabaseConfiguration,
    tableName: String,
    defaultOrderBy: String?,
    private val mapToType: (() -> Class<T>)
) : VauhtijuoksuDatabase<T> {
    private val logger = KotlinLogging.logger {}

    private val getAllQuery: String
    private val getByIdQuery: String
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

        getAllQuery = if (defaultOrderBy == null) {
            "SELECT * FROM $tableName"
        } else {
            "SELECT * FROM $tableName ORDER BY $defaultOrderBy ASC"
        }
        getByIdQuery = "SELECT * FROM $tableName WHERE id = #{id}"

        deleteQuery = client.preparedQuery("DELETE FROM $tableName WHERE id = $1")
    }

    override fun getAll(): Future<List<T>> {
        logger.debug { "Get all gamedata objects" }
        return future { p ->
            SqlTemplate.forQuery(client, getAllQuery)
                .mapTo(mapToType())
                .execute(Collections.emptyMap())
                .onFailure { t ->
                    logger.warn { "Failed to retrieve records because ${t.message}" }
                    p.fail(t)
                }
                .onSuccess { rows ->
                    p.complete(rows.toList())
                    logger.debug { "All records returned" }
                }
        }
    }

    override fun getById(id: UUID): Future<T?> {
        logger.debug { "Get record by id $id" }
        return future { p ->
            SqlTemplate.forQuery(client, getByIdQuery)
                .mapTo(mapToType())
                .execute(Collections.singletonMap("id", id) as Map<String, Any>?)
                .onFailure { t ->
                    logger.warn { "Failed to retrieve records because ${t.message}" }
                    p.fail(t)
                }
                .onSuccess { rows ->
                    if (rows.iterator().hasNext()) {
                        p.complete(rows.iterator().next())
                        logger.debug { "Found record with id $id" }
                    } else {
                        p.complete()
                        logger.debug { "No record found by id $id" }
                    }
                }
        }
    }

    override fun delete(id: UUID): Future<Boolean> {
        return future { p ->
            deleteQuery
                .execute(Tuple.of(id))
                .onFailure { t ->
                    logger.warn { "Failed to delete a record with id $id because ${t.message}" }
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
