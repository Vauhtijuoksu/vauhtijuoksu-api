package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model
import io.vertx.core.Future
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.sqlclient.PreparedQuery
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple
import io.vertx.sqlclient.templates.SqlTemplate
import mu.KotlinLogging
import java.util.Collections
import java.util.UUID

/**
 * Usable for data that uses an uuid field named id as a primary key
 */
abstract class AbstractModelDatabase<T : Model, DbModel>(
    private val client: SqlClient,
    configuration: DatabaseConfiguration,
    tableName: String,
    defaultOrderBy: String?,
    private val toModel: ((DbModel) -> T),
) : BaseDatabase(configuration), VauhtijuoksuDatabase<T> {
    private val logger = KotlinLogging.logger {}

    private val getAllQuery: String
    private val getByIdQuery: String
    private val deleteQuery: PreparedQuery<RowSet<Row>>

    init {
        getAllQuery = if (defaultOrderBy == null) {
            "SELECT * FROM $tableName"
        } else {
            "SELECT * FROM $tableName ORDER BY $defaultOrderBy ASC"
        }
        getByIdQuery = "SELECT * FROM $tableName WHERE id = #{id}"

        deleteQuery = client.preparedQuery("DELETE FROM $tableName WHERE id = $1")

        val mapper = DatabindCodec.mapper()
        mapper.registerModule(JavaTimeModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    private fun <I, R, DbModel> SqlTemplate<I, R>.mapWith(
        mapper: (SqlTemplate<I, R>) -> SqlTemplate<I, RowSet<DbModel>>
    ): SqlTemplate<I, RowSet<DbModel>> {
        return mapper(this)
    }

    abstract fun <I, R> mapToFunction(template: SqlTemplate<I, R>): SqlTemplate<I, RowSet<DbModel>>

    override fun getAll(): Future<List<T>> {
        logger.debug { "Get all objects" }
        return SqlTemplate.forQuery(client, getAllQuery)
            .mapWith(this::mapToFunction)
            .execute(Collections.emptyMap())
            .recover {
                throw ServerError("Failed to retrieve records because ${it.message}")
            }.map {
                logger.debug { "All records returned" }
                return@map it.toList().map(toModel)
            }
    }

    override fun getById(id: UUID): Future<T?> {
        logger.debug { "Get record by id $id" }
        return SqlTemplate.forQuery(client, getByIdQuery)
            .mapWith(this::mapToFunction)
            .execute(Collections.singletonMap("id", id) as Map<String, Any>?)
            .recover {
                throw ServerError("Failed to retrieve records because ${it.message}")
            }
            .map {
                if (it.iterator().hasNext()) {
                    logger.debug { "Found record with id $id" }
                    return@map toModel(it.iterator().next())
                } else {
                    logger.debug { "No record found by id $id" }
                    return@map null
                }
            }
    }

    override fun delete(id: UUID): Future<Boolean> {
        return deleteQuery
            .execute(Tuple.of(id))
            .recover {
                throw ServerError("Failed to delete a record with id $id because ${it.message}")
            }
            .map {
                return@map it.rowCount() == 1
            }
    }
}
