package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.Injector
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestIncentive
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class IncentiveDatabaseTest : VauhtijuoksuDatabaseTest<Incentive>() {
    override fun existingRecord1(): Incentive {
        return TestIncentive.incentive1
    }

    override fun existingRecord2(): Incentive {
        return TestIncentive.incentive2
    }

    override fun newRecord(): Incentive {
        return TestIncentive.incentive3
    }

    override fun tableName(): String {
        return "incentives"
    }

    override fun copyWithId(oldRecord: Incentive, newId: UUID): Incentive {
        return oldRecord.copy(id = newId)
    }

    override fun getDatabase(injector: Injector): VauhtijuoksuDatabase<Incentive> {
        return injector.getInstance(IncentiveDatabase::class.java)
    }

    @Test
    fun testUpdate() = runTest {
        val expectedIncentive = existingRecord1().copy(title = "Changed the title")
        db.update(existingRecord1().copy(title = "Changed the title"))
            .coAwait()

        db.getAll()
            .coAwait()
            .let { res ->
                assertEquals(listOf(expectedIncentive, existingRecord2()), res)
            }
    }

    @Test
    fun testUpdatingNonExistingRecord() = runTest {
        db.update(newRecord())
            .failOnSuccess()
            .recoverIfMissingEntity()
            .coAwait()

        db.getAll()
            .coAwait()
            .let { res ->
                assertEquals(listOf(existingRecord1(), existingRecord2()), res)
            }
    }
}
