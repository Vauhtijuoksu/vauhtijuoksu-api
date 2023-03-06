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
     * Get a record by id or MissingEntityException
     */
    fun getById(id: UUID): Future<T>

    fun add(record: T): Future<Unit>

    /**
     * Update given record. Note that full record is expected as input, i.e. any null values are written to db.
     *
     * Fails with MissingEntityException if the record is missing
     */
    fun update(record: T): Future<Unit>

    /**
     * Delete a record with given id or MissingEntityException
     */
    fun delete(id: UUID): Future<Unit>
}
