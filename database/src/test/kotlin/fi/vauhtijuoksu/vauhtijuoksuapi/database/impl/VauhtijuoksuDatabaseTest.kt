package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.AbstractModule
import com.google.inject.Guice
import fi.vauhtijuoksu.vauhtijuoksuapi.database.DatabaseModule
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
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
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestGameData.Companion.gameData1 as gd1
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestGameData.Companion.gameData2 as gd2
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestGameData.Companion.gameData3 as gd3

@Testcontainers
@ExtendWith(VertxExtension::class)
class VauhtijuoksuDatabaseTest {
    private lateinit var db: VauhtijuoksuDatabaseImpl
    private lateinit var sqlClient: SqlClient
    private val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    @Container
    var pg: PostgreSQLContainer<Nothing> =
        PostgreSQLContainer<Nothing>("postgres:10").withDatabaseName("vauhtijuoksu-api")

    private fun insertStatement(data: List<GameData>): String {
        fun valuesStringForGameData(gd: GameData): String {
            @Suppress("MaxLineLength")
            return "('${gd.id}', '${gd.game}', '${gd.player}', '${df.format(gd.startTime)}', '${df.format(gd.endTime)}', '${gd.category}', '${gd.device}', '${gd.published}', '${gd.vodLink}', '${gd.imgFilename}', '${gd.playerTwitch}')"
        }

        var statement = "INSERT INTO gamedata VALUES "
        for (gd in data) {
            statement += "${valuesStringForGameData(gd)},"
        }
        return statement.trim(',')
    }

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

        db = injector.getInstance(VauhtijuoksuDatabaseImpl::class.java)
        sqlClient = injector.getInstance(SqlClient::class.java)

        sqlClient.query(insertStatement(listOf(gd1, gd2)))
            .execute()
            .onFailure(testContext::failNow)
            .onSuccess {
                testContext.completeNow()
            }
    }

    @Test
    fun testGetAllOnEmptyDatabase(testContext: VertxTestContext) {
        sqlClient.query("DELETE FROM gamedata")
            .execute()
            .onFailure(testContext::failNow)
            .onSuccess {
                db.getGameData()
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
        db.getGameData()
            .onFailure(testContext::failNow)
            .onSuccess { gamedata ->
                testContext.verify {
                    assertEquals(listOf(gd1, gd2), gamedata)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testGetByNonExistingId(testContext: VertxTestContext) {
        db.getGameDataById(UUID.randomUUID())
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
        db.getGameDataById(gd2.id!!)
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(gd2, res)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testAddGameData(testContext: VertxTestContext) {
        lateinit var insertedGd: GameData
        db.addGameData(gd3)
            .onFailure(testContext::failNow)
            .onSuccess { res -> insertedGd = res }
            .compose {
                db.getGameDataById(insertedGd.id!!)
            }
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(gd3.copy(id = insertedGd.id), res)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testDeleteExisting(testContext: VertxTestContext) {
        val deletedCp = testContext.checkpoint()
        val verifiedDeletionCp = testContext.checkpoint()
        db.deleteGameData(gd2.id!!)
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertTrue(res)
                }
                deletedCp.flag()
            }
            .compose { db.getGameDataById(gd2.id!!) }
            .onSuccess { res ->
                testContext.verify {
                    assertNull(res)
                }
                verifiedDeletionCp.flag()
            }
    }

    @Test
    fun testDeleteNonExisting(testContext: VertxTestContext) {
        db.deleteGameData(UUID.randomUUID())
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertFalse(res)
                }
            }
            .compose { db.getGameData() }
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(2, res.count())
                }
                testContext.completeNow()
            }
    }
}
