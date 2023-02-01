package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.AbstractModule
import com.google.inject.Guice
import fi.vauhtijuoksu.vauhtijuoksuapi.database.DatabaseModule
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.models.StreamMetadata
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestGameData
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Testcontainers
@ExtendWith(VertxExtension::class)
class MetadataTimerDatabaseTest {
    private lateinit var db: MetadataTimerDatabase
    private lateinit var gamedataDb: GameDataDatabase

    private val emptyData = StreamMetadata(
        null,
        null,
        listOf(),
        listOf(),
        listOf(),
        listOf(),
        null,
    )

    private val someData = StreamMetadata(
        1000,
        TestGameData.gameData1.id,
        listOf("gotta go fast", "pls give money to norppas"),
        listOf(10, 100, 3),
        listOf(99, 100, 189, 69, 0),
        listOf(),
        "Deerboy - Boiiii",
    )

    private val timer1 = Timer(
        UUID.randomUUID(),
        OffsetDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2022-05-05T16:00:00Z")), ZoneId.of("Z")),
        OffsetDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2022-05-06T16:00:00Z")), ZoneId.of("Z")),
    )

    @Container
    var pg: PostgreSQLContainer<Nothing> =
        PostgreSQLContainer<Nothing>("postgres:10").withDatabaseName("vauhtijuoksu-api")

    @BeforeEach
    fun setup() {
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
                            6,
                        ),
                    )
                }
            },
        )
        db = injector.getInstance(MetadataTimerDatabase::class.java)
        gamedataDb = injector.getInstance(GameDataDatabase::class.java)
    }

    @Test
    fun `database returns empty data initially`(testContext: VertxTestContext) {
        db.get()
            .onFailure(testContext::failNow)
            .onSuccess {
                testContext.verify {
                    assertEquals(emptyData, it)
                }
                System.out.println(it)
                testContext.completeNow()
            }
    }

    @Test
    fun `database saves given data`(testContext: VertxTestContext) {
        lateinit var gameId: UUID
        gamedataDb.add(TestGameData.gameData1)
            .compose {
                gameId = it.id
                db.save(someData.copy(currentGameId = it.id, counters = listOf(1, 3, 100), timers = listOf(timer1)))
            }
            .compose { db.get() }
            .onFailure(testContext::failNow)
            .onSuccess {
                testContext.verify {
                    assertEquals(
                        someData.copy(
                            currentGameId = gameId,
                            counters = listOf(1, 3, 100),
                            timers = listOf(timer1),
                        ),
                        it,
                    )
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `current game is set to null when the game is deleted`(testContext: VertxTestContext) {
        lateinit var gameId: UUID
        gamedataDb.add(TestGameData.gameData1)
            .compose {
                gameId = it.id
                db.save(someData.copy(currentGameId = it.id))
            }
            .compose { gamedataDb.delete(gameId) }
            .compose { db.get() }
            .onFailure(testContext::failNow)
            .onSuccess {
                testContext.verify {
                    assertEquals(someData.copy(currentGameId = null), it)
                }
                testContext.completeNow()
            }
    }
}
