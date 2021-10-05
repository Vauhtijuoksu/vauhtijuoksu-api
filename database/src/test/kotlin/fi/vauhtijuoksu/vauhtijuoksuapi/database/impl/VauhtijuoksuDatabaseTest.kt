package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.AbstractModule
import com.google.inject.Guice
import fi.vauhtijuoksu.vauhtijuoksuapi.database.DatabaseModule
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(VertxExtension::class)
class VauhtijuoksuDatabaseTest {
    lateinit var db: VauhtijuoksuDatabaseImpl

    @BeforeEach
    fun beforeEach() {
        val injector = Guice.createInjector(
            DatabaseModule(),
            object : AbstractModule() {
                override fun configure() {
                    bind(DatabaseConfiguration::class.java).toInstance(DatabaseConfiguration("testaddress", 5678))
                }
            }
        )
        db = injector.getInstance(VauhtijuoksuDatabaseImpl::class.java)
    }

    @Test
    fun testGetAllOnEmptyDatabase(context: VertxTestContext) {
        db.getAll()
            .onFailure(context::failNow)
            .onSuccess { result ->
                context.verify {
                    assertTrue(result.isEmpty())
                }
                context.completeNow()
            }
    }

    @Test
    fun testGetByIdOnEmptyDatabase(context: VertxTestContext) {
        db.getById(UUID.randomUUID())
            .onFailure(context::failNow)
            .onSuccess { result ->
                context.verify {
                    assertTrue(result.isEmpty)
                }
                context.completeNow()
            }
    }
}
