package fi.vauhtijuoksu.vauhtijuoksuapi.database.api

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model
import io.vertx.core.Future
import java.util.UUID

interface VauhtijuoksuDatabase<T : Model> {
    /**
     * Get all records
     */
    fun getAll(): Future<List<T>>

    /**
     * Get a record by id
     *
     * @return Null if no such record exists
     */
    fun getById(id: UUID): Future<T?>

    fun add(record: T): Future<T>

    /**
     * Delete a record with given id
     *
     * @return Future<Boolean> indicating whether a record with given id existed, and was therefore deleted
     */
    fun delete(id: UUID): Future<Boolean>
}
