package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.Injector
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.MissingEntityException
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameParticipant
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Participant
import fi.vauhtijuoksu.vauhtijuoksuapi.models.ParticipantRole
import fi.vauhtijuoksu.vauhtijuoksuapi.models.SocialMedia
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestGameData
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestParticipant
import io.vertx.core.Future
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URL
import java.time.Instant
import java.util.Date
import java.util.UUID

class GameDataDatabaseTest : VauhtijuoksuDatabaseTest<GameData>() {
    override fun insertExistingRecords(): Future<Unit> {
        val participantDb = injector.getInstance(ParticipantDatabase::class.java)
        return participantDb.add(TestParticipant.participant1)
            .flatMap {
                participantDb.add(TestParticipant.participant2)
            }
            .flatMap {
                db.add(existingRecord1())
            }
            .flatMap {
                db.add(existingRecord2())
            }
            .mapEmpty()
    }

    override fun existingRecord1(): GameData {
        return TestGameData.gameData1
    }

    override fun existingRecord2(): GameData {
        return TestGameData.gameData2
    }

    override fun newRecord(): GameData {
        return TestGameData.gameData3
    }

    override fun tableName(): String {
        return "gamedata"
    }

    override fun copyWithId(oldRecord: GameData, newId: UUID): GameData {
        return oldRecord.copy(id = newId)
    }

    override fun getDatabase(injector: Injector): VauhtijuoksuDatabase<GameData> {
        return injector.getInstance(GameDataDatabase::class.java)
    }

    @Test
    fun testUpdate() = runTest {
        val oldId = TestGameData.gameData1.id
        val newGame = TestGameData.gameData2.copy(id = oldId)
        db.update(TestGameData.gameData2.copy(id = oldId))
            .flatMap {
                db.getById(oldId)
            }.flatMap {
                assertEquals(it, newGame)
                db.getById(TestGameData.gameData2.id)
            }.map { res ->
                assertEquals(res, TestGameData.gameData2)
                assertNotEquals(oldId, TestGameData.gameData2.id)
            }.coAwait()
    }

    @Test
    fun testUpdatingNonExistingRecord() = runTest {
        assertThrows<MissingEntityException> {
            db.update(TestGameData.gameData3).coAwait()
        }

        db.getAll().map {
            assertEquals(listOf(TestGameData.gameData1, TestGameData.gameData2), it)
        }.coAwait()
    }

    @Test
    fun testUpdateChangePlayers() = runTest {
        assertEquals(
            listOf(GameParticipant(TestParticipant.participant1.id, ParticipantRole.PLAYER)),
            existingRecord1().participants,
        )
        val playerChangedRecord = existingRecord1().copy(
            participants = listOf(
                GameParticipant(
                    TestParticipant.participant2.id,
                    ParticipantRole.PLAYER,
                ),
            ),
        )
        db.update(playerChangedRecord).flatMap {
            db.getById(playerChangedRecord.id)
        }.coAwait()
            .let {
                assertEquals(playerChangedRecord, it)
            }
    }

    @Test
    fun `player order should stay consistent when players are in multiple games`() = runTest {
        val playerDb = injector.getInstance(ParticipantDatabase::class.java)

        fun randomString(length: Int = 10, prefix: String?): String {
            val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
            return (prefix ?: "") + (1..length).map { allowedChars.random() }.joinToString("")
        }

        fun generatePlayer(): Participant {
            return Participant(
                UUID.randomUUID(),
                randomString(prefix = "name-"),
                listOf(
                    SocialMedia.twitch(randomString(prefix = "twitch-")),
                    SocialMedia.discord(randomString(prefix = "discord-")),
                ),
            )
        }

        fun generateGame(players: List<UUID>): GameData {
            return GameData(
                UUID.randomUUID(),
                randomString(prefix = "game-"),
                Date.from(Instant.now()),
                Date.from(Instant.now()),
                randomString(prefix = "category-"),
                randomString(prefix = "device-"),
                randomString(prefix = "published-"),
                URL("https://${randomString(prefix = "vodlink-")}"),
                randomString(prefix = "img-"),
                randomString(prefix = "meta-"),
                players.map { GameParticipant(it, ParticipantRole.PLAYER) },
            )
        }

        val players = (1..100).map { generatePlayer() }.associateBy { it.id }
        val games =
            (1..100).map { generateGame(players.values.shuffled().map { it.id }.subList(0, 80)) }.associateBy { it.id }

        Future.all(
            db.delete(existingRecord1().id),
            db.delete(existingRecord2().id),
        ).flatMap {
            Future.all(
                players.values.map {
                    playerDb.add(it)
                },
            )
        }.flatMap {
            Future.all(
                games.values.map {
                    db.add(it)
                },
            )
        }.flatMap {
            db.getAll()
        }.coAwait()
            .let { res ->
                res.forEach {
                    assertEquals(games[it.id], it)
                }
            }
    }
}
