package fi.vauhtijuoksu.vauhtijuoksuapi.models

import com.fasterxml.jackson.annotation.JsonIgnore
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import java.util.UUID

/**
 * Event holds data about some entity and has a sequential counter
 */
sealed class Event<T>(
    @JsonIgnore
    val counter: Long,
) {
    val type: String
        get() = this::class.simpleName ?: throw ServerError("Event type not found")

    abstract fun <U> map(mapper: ((T) -> U)): Event<U>
}

class Created<T>(
    val value: T,
    counter: Long,
) : Event<T>(counter) {
    override fun <U> map(mapper: (T) -> U): Event<U> {
        return Created(mapper(value), counter)
    }
}

class Modified<T>(
    val value: T,
    counter: Long,
) : Event<T>(counter) {
    override fun <U> map(mapper: (T) -> U): Event<U> {
        return Modified(mapper(value), counter)
    }
}

class Deleted<T>(
    val id: UUID,
    counter: Long,
) : Event<T>(counter) {
    override fun <U> map(mapper: (T) -> U): Event<U> {
        return Deleted(id, counter)
    }
}
