package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import io.vertx.core.Future
import io.vertx.core.Future.future
import mu.KotlinLogging
import java.util.Optional
import java.util.UUID
import javax.inject.Inject

class VauhtijuoksuDatabaseImpl @Inject constructor(conf: DatabaseConfiguration) : VauhtijuoksuDatabase {
    private val logger = KotlinLogging.logger {}

    init {
        logger.debug("Not connecting to ${conf.address}:${conf.port}")
    }

    override fun getAll(): Future<List<GameData>> {
        logger.debug { "Get all gamedata objects" }
        return future { p ->
            p.complete(arrayListOf())
            logger.debug { "All of gamedata objects returned" }
        }
    }

    override fun getById(id: UUID): Future<Optional<GameData>> {
        return future { p ->
            p.complete(Optional.empty())
        }
    }
}
