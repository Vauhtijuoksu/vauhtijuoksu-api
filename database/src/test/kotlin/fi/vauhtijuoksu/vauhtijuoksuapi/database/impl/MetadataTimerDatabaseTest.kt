package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.AbstractModule
import com.google.inject.Guice
import fi.vauhtijuoksu.vauhtijuoksuapi.database.DatabaseModule
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameParticipant
import fi.vauhtijuoksu.vauhtijuoksuapi.models.ParticipantRole
import fi.vauhtijuoksu.vauhtijuoksuapi.models.StreamMetadata
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestGameData
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestParticipant
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class MetadataTimerDatabaseTest {
    private lateinit var db: StreamMetadataDatabase
    private lateinit var gameDataDb: GameDataDatabase
    private lateinit var participantDatabase: ParticipantDatabase

    private val emptyData =
        StreamMetadata(
            null,
            null,
            listOf(),
            listOf(),
            listOf(),
            null,
        )

    private val someData =
        StreamMetadata(
            1000,
            TestGameData.gameData1.id,
            listOf("gotta go fast", "pls give money to norppas"),
            listOf(10, 100, 3),
            listOf(99, 100, 189, 69, 0),
            "Deerboy - Boiiii",
        )

    @Container
    var pg: PostgreSQLContainer<Nothing> =
        PostgreSQLContainer<Nothing>("postgres:10").withDatabaseName("vauhtijuoksu-api")

    @BeforeEach
    fun setup() {
        val injector =
            Guice.createInjector(
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
        db = injector.getInstance(StreamMetadataDatabase::class.java)
        gameDataDb = injector.getInstance(GameDataDatabase::class.java)
        participantDatabase = injector.getInstance(ParticipantDatabase::class.java)
    }

    @Test
    fun `database returns empty data initially`() =
        runTest {
            db
                .get()
                .coAwait()
                .let {
                    assertEquals(emptyData, it)
                }
        }

    @Test
    fun `database saves given data`() =
        runTest {
            val gameData = TestGameData.gameData1
            assertEquals(1, gameData.participants.size)
            assertEquals(GameParticipant(TestParticipant.participant1.id, ParticipantRole.PLAYER), gameData.participants.first())
            participantDatabase
                .add(TestParticipant.participant1)
                .compose {
                    gameDataDb.add(gameData)
                }.compose {
                    db.save(
                        someData.copy(
                            currentGameId = gameData.id,
                            counters = listOf(1, 3, 100),
                        ),
                    )
                }.compose { db.get() }
                .coAwait()
                .let {
                    assertEquals(
                        someData.copy(
                            currentGameId = gameData.id,
                            counters = listOf(1, 3, 100),
                        ),
                        it,
                    )
                }
        }

    @Test
    fun `current game is set to null when the game is deleted`() =
        runTest {
            val gameData = TestGameData.gameData1
            assertEquals(1, gameData.participants.size)
            assertEquals(GameParticipant(TestParticipant.participant1.id, ParticipantRole.PLAYER), gameData.participants.first())
            participantDatabase
                .add(TestParticipant.participant1)
                .compose {
                    gameDataDb.add(TestGameData.gameData1)
                }.compose {
                    db.save(someData.copy(currentGameId = gameData.id))
                }.compose { gameDataDb.delete(gameData.id) }
                .compose { db.get() }
                .coAwait()
                .let {
                    assertEquals(someData.copy(currentGameId = null), it)
                }
        }
}
