package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.Injector
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestGameData
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class GameDataDatabaseTest : VauhtijuoksuDatabaseTest<GameData>() {
    override fun insertStatement(data: List<GameData>): String {
        fun valuesStringForGameData(gd: GameData): String {
            @Suppress("MaxLineLength")
            return "('${gd.id}', '${gd.game}', '${gd.player}', '${df.format(gd.startTime)}', '${df.format(gd.endTime)}', '${gd.category}', '${gd.device}', '${gd.published}', '${gd.vodLink}', '${gd.imgFilename}', '${gd.playerTwitch}', '${gd.meta}')"
        }

        var statement = "INSERT INTO gamedata VALUES "
        for (gd in data) {
            statement += "${valuesStringForGameData(gd)},"
        }
        return statement.trim(',')
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
            .map { res ->
                testContext.verify {
                    assertEquals(res, newGame)
                }
            }.compose {
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
}
