package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.MissingEntityException
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import io.vertx.core.Future
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlResult
import io.vertx.sqlclient.templates.SqlTemplate

internal fun <I, R, DbModel> SqlTemplate<I, R>.mapWith(
    mapper: (SqlTemplate<I, R>) -> SqlTemplate<I, RowSet<DbModel>>,
): SqlTemplate<I, RowSet<DbModel>> {
    return mapper(this)
}

internal fun <T, V : SqlResult<T>> Future<V>.expectOneChangedRow(): Future<Unit> {
    return this.map {
        if (it.rowCount() == 0) {
            throw MissingEntityException("No row found")
        } else if (it.rowCount() > 1) {
            throw ServerError("Found more than one row")
        }
        return@map
    }
}
