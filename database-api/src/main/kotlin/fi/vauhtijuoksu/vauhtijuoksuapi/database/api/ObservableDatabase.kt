package fi.vauhtijuoksu.vauhtijuoksuapi.database.api

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Event
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model
import kotlinx.coroutines.flow.Flow

sealed interface ObservableDatabase<T> {
    val flow: Flow<Event<T>>
}

interface ObservableVauhtijuoksuDatabase<T : Model> : ObservableDatabase<T>, VauhtijuoksuDatabase<T> {
    suspend fun getAllAndCounter(): Pair<Long, List<T>>
}

interface ObservableSingletonDatabase<T> : ObservableDatabase<T>, SingletonDatabase<T> {
    suspend fun getAndCounter(): Pair<Long, T>
}
