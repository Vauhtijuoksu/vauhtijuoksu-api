package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import fi.vauhtijuoksu.vauhtijuoksuapi.database.DatabaseModule
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model
import io.vertx.core.Future
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.SqlClient
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.text.SimpleDateFormat
import java.util.UUID

@Testcontainers
abstract class VauhtijuoksuDatabaseTest<T : Model> {
    protected lateinit var db: VauhtijuoksuDatabase<T>
    private lateinit var sqlClient: SqlClient
    protected lateinit var injector: Injector
    val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    @Container
    var pg: PostgreSQLContainer<Nothing> =
        PostgreSQLContainer<Nothing>("postgres:16.8").withDatabaseName("vauhtijuoksu-api")

    abstract fun existingRecord1(): T
    abstract fun existingRecord2(): T
    abstract fun newRecord(): T
    abstract fun tableName(): String
    abstract fun copyWithId(oldRecord: T, newId: UUID): T
    abstract fun getDatabase(injector: Injector): VauhtijuoksuDatabase<T>

    @BeforeEach
    fun beforeEach() = runTest {
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

        insertExistingRecords().coAwait()
    }

    protected open fun insertExistingRecords(): Future<Unit> {
        return db.add(existingRecord1())
            .flatMap {
                db.add(existingRecord2())
            }.mapEmpty()
    }

    @Test
    fun testGetAllOnEmptyDatabase() = runTest {
        sqlClient.query("DELETE FROM ${tableName()}")
            .execute()
            .coAwait()

        db.getAll()
            .coAwait()
            .let { result ->
                assertTrue(result.isEmpty())
            }
    }

    @Test
    fun testGetAll() = runTest {
        db.getAll()
            .coAwait()
            .let { records ->
                assertEquals(listOf(existingRecord1(), existingRecord2()), records)
            }
    }

    @Test
    fun testGetByNonExistingId() = runTest {
        db.getById(UUID.randomUUID())
            .failOnSuccess()
            .recoverIfMissingEntity()
            .coAwait()
    }

    @Test
    fun testGetById() = runTest {
        db.getById(existingRecord1().id)
            .coAwait()
            .let { res ->
                assertEquals(existingRecord1(), res)
            }
    }

    @Test
    fun testAdd() = runTest {
        val insertedRecord = newRecord()
        db.add(insertedRecord)
            .flatMap {
                db.getById(insertedRecord.id)
            }
            .coAwait()
            .let { res ->
                assertEquals(insertedRecord, res)
            }
    }

    @Test
    fun testDeleteExisting() = runTest {
        db.delete(existingRecord1().id)
            .coAwait()

        db.getById(existingRecord1().id)
            .failOnSuccess()
            .recoverIfMissingEntity()
            .coAwait()
    }

    @Test
    fun testDeleteNonExisting() = runTest {
        db.delete(UUID.randomUUID())
            .failOnSuccess()
            .recoverIfMissingEntity()
            .coAwait()

        db.getAll()
            .coAwait()
            .let { res ->
                assertEquals(2, res.count())
            }
    }
}
