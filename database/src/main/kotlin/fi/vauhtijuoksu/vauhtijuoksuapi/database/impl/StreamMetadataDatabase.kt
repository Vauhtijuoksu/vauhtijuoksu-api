package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.SingletonDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.database.models.StreamMetadataDbModel
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.StreamMetadata
import io.vertx.core.Future
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple
import io.vertx.sqlclient.templates.SqlTemplate
import java.util.Collections
import javax.inject.Inject

class StreamMetadataDatabase
@Inject constructor(
    configuration: DatabaseConfiguration,
    private val client: SqlClient,
) : BaseDatabase(configuration),
    SingletonDatabase<StreamMetadata> {
    override fun get(): Future<StreamMetadata> {
        return SqlTemplate.forQuery(client, "SELECT * FROM stream_metadata LIMIT 1")
            .mapTo(StreamMetadataDbModel::class.java)
            .execute(Collections.emptyMap())
            .recover {
                throw ServerError("Failed to get stream metadata: ${it.message}")
            }
            .map {
                return@map it.first().toStreamMetadata()
            }
    }

    override fun save(record: StreamMetadata): Future<Void> {
        return client.preparedQuery(
            """UPDATE stream_metadata SET 
                        donation_goal = $1,
                        current_game_id = $2,
                        donatebar_info = $3,
                        counters = $4,
                        heart_rates = $5,
                        now_playing = $6
                        """,
        ).execute(
            Tuple.of(
                record.donationGoal,
                record.currentGameId,
                record.donateBarInfo.toTypedArray(),
                record.counters.toTypedArray(),
                record.heartRates.toTypedArray(),
                record.nowPlaying,
            ),
        ).recover {
            throw ServerError(it)
        }.mapEmpty()
    }
}
