package fi.vauhtijuoksu.vauhtijuoksuapi.featuretest

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.ext.web.client.HttpRequest
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.lang.annotation.Inherited

class FeatureTestUtils : ParameterResolver {
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
