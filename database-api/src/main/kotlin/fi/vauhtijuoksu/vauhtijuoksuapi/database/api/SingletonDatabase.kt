package fi.vauhtijuoksu.vauhtijuoksuapi.database.api

import io.vertx.core.Future

interface SingletonDatabase<T> {
    /**
     * Get a record if one exists
     *
     * @return Null if no such record exists
     */
    fun get(): Future<T>

    /**
     * Insert or update given record and return the record.
     * Note that full record is expected as input, i.e. any null values are written to db.
     */
    fun save(record: T): Future<Void>
}
