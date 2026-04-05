package fi.vauhtijuoksu.vauhtijuoksuapi.featuretest

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.ext.web.client.HttpRequest
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.lang.annotation.Inherited

private const val POLL_INTERVAL_MS = 50L

class FeatureTestUtils : ParameterResolver, BeforeAllCallback {
    override fun beforeAll(extensionContext: ExtensionContext) = runTest {
            val client = HttpClient(CIO)
            try {
                while (true) {
                    val ok = runCatching {
                        client.get("http://api.localhost/gamedata").status == HttpStatusCode.OK
                    }.getOrDefault(false)
                    if (ok) break
                    delay(POLL_INTERVAL_MS)
                }
            } finally {
                client.close()
            }
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type.equals(WebClient::class.java)
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        val vertx = Vertx.vertx()
        val webClient = WebClient.create(
            vertx,
            WebClientOptions()
                .setDefaultHost("api.localhost"),
        )
        extensionContext.getStore(
            ExtensionContext.Namespace.create(
                extensionContext.requiredTestClass,
                extensionContext.requiredTestMethod,
            ),
        ).put(
            "data",
            object : CloseableResource {
                override fun close() {
                    webClient.close()
                    vertx.close()
                }
            },
        )
        return webClient
    }
}

fun <T> HttpRequest<T>.withAuthenticationAndOrigins(): HttpRequest<T> {
    return this.authentication(UsernamePasswordCredentials("vauhtijuoksu", "vauhtijuoksu"))
        .putHeader("Origin", "http://api.localhost")
}

fun <T> Future<HttpResponse<T>>.verifyStatusCode(
    expectedCode: Int,
    testContext: VertxTestContext,
): Future<HttpResponse<T>> {
    return this.map {
        testContext.verify {
            assertEquals(expectedCode, it.statusCode())
        }
        it
    }
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@ExtendWith(VertxExtension::class)
@ExtendWith(FeatureTestUtils::class)
annotation class FeatureTest
