package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import io.vertx.core.Future
import io.vertx.core.Future.future
import java.util.Optional
import java.util.UUID
import javax.inject.Inject

class VauhtijuoksuDatabaseImpl @Inject constructor(conf: DatabaseConfiguration) : VauhtijuoksuDatabase {
    init {
        println("Not connecting to ${conf.address}:${conf.port}")
    }

    override fun getAll(): Future<List<GameData>> {
        return future { p ->
            p.complete(arrayListOf())
        }
    }

    override fun getById(id: UUID): Future<Optional<GameData>> {
        return future { p ->
            p.complete(Optional.empty())
        }
    }
}
