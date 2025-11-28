package fi.vauhtijuoksu.vauhtijuoksuapi

import io.vertx.core.Future
import io.vertx.junit5.VertxTestContext
import org.mockito.Mockito

class MockitoUtils private constructor() {
    companion object {
        // Mockito returns null with any(). This fails on non-nullable parameters
        // Stackoverflow taught me a workaround https://stackoverflow.com/questions/30305217/is-it-possible-to-use-mockito-in-kotlin
        @Suppress("UNCHECKED_CAST")
        private fun <T> uninitialized(): T = null as T

        fun <T> any(): T {
            Mockito.any<T>()
            return uninitialized()
        }
    }
}

fun <T> Future<T>.verify(
    testContext: VertxTestContext,
    verifications: (T) -> Unit,
): Future<T> {
    this
        .onFailure(testContext::failNow)
        .onSuccess {
            testContext.verify {
                verifications(it)
            }
        }
    return this
}

fun <T> Future<T>.verifyAndCompleteTest(
    testContext: VertxTestContext,
    verifications: (T) -> Unit,
) {
    verify(testContext, verifications)
        .onSuccess { testContext.completeNow() }
}
