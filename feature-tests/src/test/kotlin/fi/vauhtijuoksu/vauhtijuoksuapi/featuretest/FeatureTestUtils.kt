package fi.vauhtijuoksu.vauhtijuoksuapi.featuretest

import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.junit5.VertxExtension
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
                .setDefaultHost("api.localhost")
        )
        extensionContext.getStore(
            ExtensionContext.Namespace.create(
                extensionContext.requiredTestClass,
                extensionContext.requiredTestMethod
            )
        ).put(
            "data",
            object : CloseableResource {
                override fun close() {
                    webClient.close()
                    vertx.close()
                }
            }
        )
        return webClient
    }
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@ExtendWith(VertxExtension::class)
@ExtendWith(FeatureTestUtils::class)
annotation class FeatureTest
