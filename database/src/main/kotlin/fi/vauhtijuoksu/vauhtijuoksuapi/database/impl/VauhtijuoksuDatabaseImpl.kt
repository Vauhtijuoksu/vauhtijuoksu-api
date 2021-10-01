package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import io.vertx.core.Future
import io.vertx.core.Future.future
import java.util.Optional
import java.util.UUID

class VauhtijuoksuDatabaseImpl : VauhtijuoksuDatabase {
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
