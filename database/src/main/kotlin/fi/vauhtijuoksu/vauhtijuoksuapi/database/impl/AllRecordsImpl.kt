package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.AllRecords
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.ObservableResponse
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Event
import kotlinx.coroutines.flow.Flow

class AllRecordsImpl<T> : AllRecords<T> {
    override suspend fun getAll(): ObservableResponse<List<T>> {
        TODO("Not yet implemented")
    }

    override val flow: Flow<Event<T>>
        get() = TODO("Not yet implemented")
}
