package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.Injector
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestGameData
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestPlayer
import io.vertx.core.Future
import io.vertx.junit5.VertxTestContext
import io.vertx.sqlclient.SqlClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class GameDataDatabaseTest : VauhtijuoksuDatabaseTest<GameData>() {

    override fun insertStatement(data: List<GameData>): String {
        fun valuesStringForGameData(gd: GameData): String {
            @Suppress("MaxLineLength")
            return "('${gd.id}', '${gd.game}', '${df.format(gd.startTime)}', '${df.format(gd.endTime)}', '${gd.category}', '${gd.device}', '${gd.published}', '${gd.vodLink}', '${gd.imgFilename}', '${gd.meta}')"
        }

        var statement = "INSERT INTO gamedata VALUES "
        for (gd in data) {
            statement += "${valuesStringForGameData(gd)},"
        }
        return statement.trim(',')
    }

    override fun insertExistingRecords(): Future<Unit> {
        val playerDb = injector.getInstance(PlayerDatabase::class.java)
        val client = injector.getInstance(SqlClient::class.java)
        return playerDb
            .add(TestPlayer.player1)
            .flatMap {
                playerDb.add(TestPlayer.player2)
            }.flatMap {
                super.insertExistingRecords()
            }
            .flatMap {
                client
                    .query(
                        @Suppress("MaxLineLength")
                        """INSERT INTO players_in_game (game_id, player_id) VALUES ('${existingRecord1().id}', '${existingRecord1().players.first()}'), ('${existingRecord2().id}', '${existingRecord2().players.first()}')""",
                    )
                    .execute()
                    .mapEmpty()
            }
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
    fun testUpdate(testContext: VertxTestContext) {
        val oldId = TestGameData.gameData1.id
        val newGame = TestGameData.gameData2.copy(id = oldId)
        db.update(TestGameData.gameData2.copy(id = oldId))
            .compose {
                db.getById(oldId)
            }
            .map {
                testContext.verify {
                    assertEquals(it, newGame)
                }
            }
            .compose {
                db.getById(TestGameData.gameData2.id)
            }
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(res, TestGameData.gameData2)
                    assertNotEquals(oldId, TestGameData.gameData2.id)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testUpdatingNonExistingRecord(testContext: VertxTestContext) {
        db.update(TestGameData.gameData3)
            .failOnSuccess(testContext)
            .recoverIfMissingEntity(testContext)
            .compose { db.getAll() }
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(listOf(TestGameData.gameData1, TestGameData.gameData2), res)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testUpdateChangePlayers(testContext: VertxTestContext) {
        assertEquals(listOf(TestPlayer.player1.id), existingRecord1().players)
        val playerChangedRecord = existingRecord1().copy(players = listOf(TestPlayer.player2.id))
        db.update(playerChangedRecord)
            .compose {
                db.getById(playerChangedRecord.id)
            }
            .map {
                testContext.verify {
                    assertEquals(playerChangedRecord, it)
                }
                testContext.completeNow()
            }
            .onFailure(testContext::failNow)
    }
}
