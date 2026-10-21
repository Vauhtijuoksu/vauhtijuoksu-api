package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.MissingEntityException
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.UserError
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.VauhtijuoksuException
import io.vertx.core.Future
import io.vertx.pgclient.PgException
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlResult
import io.vertx.sqlclient.templates.SqlTemplate

internal fun <I, R, DbModel> SqlTemplate<I, R>.mapWith(
    mapper: (SqlTemplate<I, R>) -> SqlTemplate<I, RowSet<DbModel>>,
): SqlTemplate<I, RowSet<DbModel>> {
    return mapper(this)
}

internal fun <V> Future<V>.orServerError(): Future<V> {
    return this.recover {
        if (it is VauhtijuoksuException) {
            throw it
        } else {
            throw ServerError(it)
        }
    }
}

private const val FOREIGN_KEY_VIOLATION = "23503"

// Foreign key violations are caused by the input, e.g. an unknown charity
internal fun databaseError(error: Throwable, message: String): VauhtijuoksuException {
    return if (error is PgException && error.sqlState == FOREIGN_KEY_VIOLATION) {
        UserError("$message: ${error.errorMessage}. ${error.detail}")
    } else {
        ServerError("$message because of ${error.message}")
    }
}

internal inline fun <reified T> mapperToType(): (Row) -> T {
    return { it.toJson().mapTo(T::class.java) }
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
