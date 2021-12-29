package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.SingletonDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.database.models.StreamMetadataDbModel
import fi.vauhtijuoksu.vauhtijuoksuapi.models.StreamMetadata
import io.vertx.core.Future
import io.vertx.core.Future.future
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple
import io.vertx.sqlclient.templates.SqlTemplate
import java.util.Collections
import javax.inject.Inject

class StreamMetadataDatabase
@Inject constructor(
    configuration: DatabaseConfiguration,
    private val client: SqlClient,
) :
    BaseDatabase(configuration),
    SingletonDatabase<StreamMetadata> {

    override fun get(): Future<StreamMetadata> {
        return future { p ->
            SqlTemplate.forQuery(client, "SELECT * FROM stream_metadata LIMIT 1")
                .mapTo(StreamMetadataDbModel::class.java)
                .execute(Collections.emptyMap())
                .onFailure(p::fail)
                .onSuccess { rows ->
                    p.complete(rows.first().toStreamMetadata())
                }
        }
    }

    override fun save(record: StreamMetadata): Future<Void> {
        return client.preparedQuery(
            """UPDATE stream_metadata SET 
                        donation_goal = $1,
                        current_game_id = $2,
                        donatebar_info = $3,
                        counters = $4
                        """
        ).execute(
            Tuple.of(
                record.donationGoal,
                record.currentGameId,
                record.donateBarInfo.toTypedArray(),
                record.counters.toTypedArray()
            )
        ).map {
            return@map null
        }
    }
}
