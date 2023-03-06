package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.MissingEntityException
import io.vertx.core.Future
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertTrue

internal fun <V> Future<V>.failOnSuccess(testContext: VertxTestContext): Future<V> {
    return this.onSuccess { testContext.failNow("Expected to fail") }
}

internal fun <V> Future<V>.recoverIfMissingEntity(testContext: VertxTestContext): Future<V> {
    return this.recover {
        testContext.verify {
            assertTrue(it is MissingEntityException)
        }
        return@recover Future.succeededFuture()
    }
}

internal fun <V> Future<V>.completeOnSuccessOrFail(testContext: VertxTestContext): Future<V> {
    return this.onSuccess {
        testContext.completeNow()
    }.onFailure(testContext::failNow)
}
