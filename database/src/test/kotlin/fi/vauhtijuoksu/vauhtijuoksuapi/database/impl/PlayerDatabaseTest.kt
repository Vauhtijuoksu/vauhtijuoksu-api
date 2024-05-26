package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.Injector
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Player
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestPlayer
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class PlayerDatabaseTest : VauhtijuoksuDatabaseTest<Player>() {
    override fun existingRecord1(): Player {
        return TestPlayer.player1
    }

    override fun existingRecord2(): Player {
        return TestPlayer.player2
    }

    override fun newRecord(): Player {
        return TestPlayer.player3
    }

    override fun tableName(): String {
        return "players"
    }

    override fun copyWithId(oldRecord: Player, newId: UUID): Player {
        return oldRecord.copy(id = newId)
    }

    override fun getDatabase(injector: Injector): VauhtijuoksuDatabase<Player> {
        return injector.getInstance(PlayerDatabase::class.java)
    }

    @Test
    fun testUpdate() = runTest {
        val oldId = existingRecord1().id
        val newPlayer = existingRecord2().copy(id = oldId)
        db.update(newPlayer)
            .flatMap {
                db.getAll()
            }
            .coAwait()
            .let {
                assertEquals(listOf(existingRecord2(), newPlayer), it)
            }
    }

    @Test
    fun testUpdateNonExisting() = runTest {
        val newUuid = UUID.randomUUID()
        db.update(existingRecord2().copy(id = newUuid))
            .failOnSuccess()
            .recoverIfMissingEntity()
            .coAwait()

        db.getAll()
            .coAwait()
            .let {
                assertEquals(listOf(existingRecord1(), existingRecord2()), it)
            }
    }
}
