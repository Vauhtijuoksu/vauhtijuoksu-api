package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.Injector
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Participant
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestParticipant
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class ParticipantDatabaseTest : VauhtijuoksuDatabaseTest<Participant>() {
    override fun existingRecord1(): Participant = TestParticipant.participant1

    override fun existingRecord2(): Participant = TestParticipant.participant2

    override fun newRecord(): Participant = TestParticipant.participant3

    override fun tableName(): String = "participants"

    override fun copyWithId(
        oldRecord: Participant,
        newId: UUID,
    ): Participant = oldRecord.copy(id = newId)

    override fun getDatabase(injector: Injector): VauhtijuoksuDatabase<Participant> = injector.getInstance(ParticipantDatabase::class.java)

    @Test
    fun testUpdate() =
        runTest {
            val oldId = existingRecord1().id
            val newPlayer = newRecord().copy(id = oldId)
            db
                .update(newPlayer)
                .coAwait()

            db
                .getAll()
                .coAwait()
                .let {
                    assertEquals(listOf(newPlayer, existingRecord2()).sortedBy { it.displayName }, it)
                }
        }

    @Test
    fun testUpdateNonExisting() =
        runTest {
            val newUuid = UUID.randomUUID()
            db
                .update(existingRecord2().copy(id = newUuid))
                .failOnSuccess()
                .recoverIfMissingEntity()
                .coAwait()

            db
                .getAll()
                .coAwait()
                .let {
                    assertEquals(listOf(existingRecord1(), existingRecord2()), it)
                }
        }
}
