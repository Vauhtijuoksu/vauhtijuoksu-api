package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.MissingEntityException
import io.vertx.core.Future
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.fail

internal fun <V> Future<V>.failOnSuccess(): Future<V> {
    return this.onSuccess {
        fail("Expected to fail")
    }
}

internal fun <V> Future<V>.recoverIfMissingEntity(): Future<V> {
    return this.recover {
        assertTrue(it is MissingEntityException)
        return@recover Future.succeededFuture()
    }
}
