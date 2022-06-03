package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import fi.vauhtijuoksu.vauhtijuoksuapi.database.DatabaseModule
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model
import io.vertx.junit5.Timeout
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.sqlclient.SqlClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
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
@Timeout(1000)
abstract class VauhtijuoksuDatabaseTest<T : Model> {
    protected lateinit var db: VauhtijuoksuDatabase<T>
    private lateinit var sqlClient: SqlClient
    val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    @Container
    var pg: PostgreSQLContainer<Nothing> =
        PostgreSQLContainer<Nothing>("postgres:10").withDatabaseName("vauhtijuoksu-api")

    abstract fun insertStatement(data: List<T>): String

    abstract fun existingRecord1(): T
    abstract fun existingRecord2(): T
    abstract fun newRecord(): T
    abstract fun tableName(): String
    abstract fun copyWithId(oldRecord: T, newId: UUID): T
    abstract fun getDatabase(injector: Injector): VauhtijuoksuDatabase<T>

    @BeforeEach
    fun beforeEach(testContext: VertxTestContext) {
        val injector = Guice.createInjector(
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
                            6
                        )
                    )
                }
            }
        )

        db = getDatabase(injector)
        sqlClient = injector.getInstance(SqlClient::class.java)

        sqlClient.query(insertStatement(listOf(existingRecord1(), existingRecord2())))
            .execute()
            .onFailure(testContext::failNow)
            .onSuccess {
                testContext.completeNow()
            }
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
            .onFailure(testContext::failNow)
            .onSuccess { result ->
                testContext.verify {
                    assertNull(result)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testGetById(testContext: VertxTestContext) {
        db.getById(existingRecord1().id!!)
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
        lateinit var insertedRecord: T
        db.add(newRecord())
            .onFailure(testContext::failNow)
            .onSuccess { res -> insertedRecord = res }
            .compose {
                db.getById(insertedRecord.id!!)
            }
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(copyWithId(newRecord(), insertedRecord.id!!), res)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testDeleteExisting(testContext: VertxTestContext) {
        val deletedCp = testContext.checkpoint()
        val verifiedDeletionCp = testContext.checkpoint()
        db.delete(existingRecord1().id!!)
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertTrue(res)
                }
                deletedCp.flag()
            }
            .compose { db.getById(existingRecord1().id!!) }
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertNull(res)
                }
                verifiedDeletionCp.flag()
            }
    }

    @Test
    fun testDeleteNonExisting(testContext: VertxTestContext) {
        db.delete(UUID.randomUUID())
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertFalse(res)
                }
            }
            .compose { db.getAll() }
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(2, res.count())
                }
                testContext.completeNow()
            }
    }
}
