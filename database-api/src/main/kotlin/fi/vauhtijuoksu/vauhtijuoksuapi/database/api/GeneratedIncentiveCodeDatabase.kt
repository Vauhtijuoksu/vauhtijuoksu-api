package fi.vauhtijuoksu.vauhtijuoksuapi.database.api

import fi.vauhtijuoksu.vauhtijuoksuapi.models.GeneratedIncentive
import io.vertx.core.Future

interface GeneratedIncentiveCodeDatabase {
    fun getAll(): Future<List<GeneratedIncentive>>

    fun add(record: GeneratedIncentive): Future<Void>
}
