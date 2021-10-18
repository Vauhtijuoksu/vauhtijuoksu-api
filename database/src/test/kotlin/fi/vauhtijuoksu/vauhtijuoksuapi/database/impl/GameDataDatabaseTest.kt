package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.Injector
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestGameData
import java.util.UUID

class GameDataDatabaseTest : VauhtijuoksuDatabaseTest<GameData>() {
    override fun insertStatement(data: List<GameData>): String {
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
}
