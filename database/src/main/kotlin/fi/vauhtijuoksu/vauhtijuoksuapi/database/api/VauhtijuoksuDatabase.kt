package fi.vauhtijuoksu.vauhtijuoksuapi.database.api

import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import io.vertx.core.Future
import java.util.Optional
import java.util.UUID

interface VauhtijuoksuDatabase {
    fun getAll(): Future<List<GameData>>
    fun getById(id: UUID): Future<Optional<GameData>>
}
