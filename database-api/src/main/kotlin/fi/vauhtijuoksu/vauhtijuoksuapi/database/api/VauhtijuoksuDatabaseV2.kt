package fi.vauhtijuoksu.vauhtijuoksuapi.database.api

import arrow.core.Either
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.MissingEntityError
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.VauhtijuoksuError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Event
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface ObservableResponse<T> {
    val counter: Long
    val data: T
}

interface Observable<T> {
    val flow: Flow<Event<T>>
}

interface AllRecords<T> : Observable<T> {
    suspend fun getAll(): ObservableResponse<List<T>>
}

interface SingleRecord<T> : Observable<T> {
    suspend fun get(id: UUID): Either<MissingEntityError, ObservableResponse<T>>
}

interface OnlyRecord<T> {
    suspend fun get(): ObservableResponse<T>
}

interface AddRecord<T> {
    suspend fun add(record: T)
}

interface UpdateRecord<T> {
    suspend fun update(record: T): Either<VauhtijuoksuError, Unit>
}

interface DeleteRecord {
    suspend fun delete(id: UUID): Either<VauhtijuoksuError, Unit>
}

interface VauhtijuoksuDatabaseV2<T> : AllRecords<T>, SingleRecord<T>, AddRecord<T>, UpdateRecord<T>, DeleteRecord
