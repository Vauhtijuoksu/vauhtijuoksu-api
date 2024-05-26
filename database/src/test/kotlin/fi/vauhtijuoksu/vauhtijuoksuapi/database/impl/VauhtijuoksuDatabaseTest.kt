package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import fi.vauhtijuoksu.vauhtijuoksuapi.database.DatabaseModule
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.MissingEntityException
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model
import io.vertx.core.Future
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.sqlclient.SqlClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.text.SimpleDateFormat
import java.util.UUID

@Testcontainers
@ExtendWith(VertxExtension::class)
abstract class VauhtijuoksuDatabaseTest<T : Model> {
    protected lateinit var db: VauhtijuoksuDatabase<T>
    private lateinit var sqlClient: SqlClient
    protected lateinit var injector: Injector
    val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    @Container
    var pg: PostgreSQLContainer<Nothing> =
        PostgreSQLContainer<Nothing>("postgres:10").withDatabaseName("vauhtijuoksu-api")

    abstract fun existingRecord1(): T
    abstract fun existingRecord2(): T
    abstract fun newRecord(): T
    abstract fun tableName(): String
    abstract fun copyWithId(oldRecord: T, newId: UUID): T
    abstract fun getDatabase(injector: Injector): VauhtijuoksuDatabase<T>

    @BeforeEach
    fun beforeEach(testContext: VertxTestContext) {
        injector = Guice.createInjector(
            DatabaseModule(),
            object : AbstractModule() {
                override fun configure() {
                    bind(DatabaseConfiguration::class.java).toInstance(
                        DatabaseConfiguration(
                            pg.host,
                            pg.firstMappedPort,
                            "vauhtijuoksu-api",
                            pg.username,
                            pg.password,
                            6,
                        ),
                    )
                }
            },
        )

        db = getDatabase(injector)
        sqlClient = injector.getInstance(SqlClient::class.java)

        insertExistingRecords()
            .onFailure(testContext::failNow)
            .onSuccess {
                testContext.completeNow()
            }
    }

    protected open fun insertExistingRecords(): Future<Unit> {
        return db.add(existingRecord1())
            .flatMap {
                db.add(existingRecord2())
            }.mapEmpty()
    }

    @Test
    fun testGetAllOnEmptyDatabase(testContext: VertxTestContext) {
        sqlClient.query("DELETE FROM ${tableName()}")
            .execute()
            .onFailure(testContext::failNow)
            .onSuccess {
                db.getAll()
                    .onFailure(testContext::failNow)
                    .onSuccess { result ->
                        testContext.verify {
                            assertTrue(result.isEmpty())
                        }
                        testContext.completeNow()
                    }
            }
    }

    @Test
    fun testGetAll(testContext: VertxTestContext) {
        db.getAll()
            .onFailure(testContext::failNow)
            .onSuccess { records ->
                testContext.verify {
                    assertEquals(listOf(existingRecord1(), existingRecord2()), records)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testGetByNonExistingId(testContext: VertxTestContext) {
        db.getById(UUID.randomUUID())
            .failOnSuccess(testContext)
            .recoverIfMissingEntity(testContext)
            .completeOnSuccessOrFail(testContext)
    }

    @Test
    fun testGetById(testContext: VertxTestContext) {
        db.getById(existingRecord1().id)
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(existingRecord1(), res)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testAdd(testContext: VertxTestContext) {
        val insertedRecord = newRecord()
        db.add(insertedRecord)
            .flatMap {
                db.getById(insertedRecord.id)
            }
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(insertedRecord, res)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testDeleteExisting(testContext: VertxTestContext) {
        val deletedCp = testContext.checkpoint()
        val verifiedDeletionCp = testContext.checkpoint()
        db.delete(existingRecord1().id)
            .onFailure(testContext::failNow)
            .map { deletedCp.flag() }
            .compose { db.getById(existingRecord1().id) }
            .onFailure {
                testContext.verify {
                    assertTrue(it is MissingEntityException)
                }
                verifiedDeletionCp.flag()
            }
    }

    @Test
    fun testDeleteNonExisting(testContext: VertxTestContext) {
        db.delete(UUID.randomUUID())
            .failOnSuccess(testContext)
            .recoverIfMissingEntity(testContext)
            .compose { db.getAll() }
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(2, res.count())
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }
}
