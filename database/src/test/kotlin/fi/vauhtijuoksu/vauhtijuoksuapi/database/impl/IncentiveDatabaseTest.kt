package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.Injector
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestIncentive
import io.vertx.junit5.VertxTestContext
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
    fun testUpdate(testContext: VertxTestContext) {
        val expectedIncentive = existingRecord1().copy(title = "Changed the title")
        db.update(existingRecord1().copy(title = "Changed the title"))
            .onFailure(testContext::failNow)
            .compose { db.getAll() }
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(listOf(expectedIncentive, existingRecord2()), res)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testUpdatingNonExistingRecord(testContext: VertxTestContext) {
        db.update(newRecord())
            .failOnSuccess(testContext)
            .recoverIfMissingEntity(testContext)
            .compose { db.getAll() }
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(listOf(existingRecord1(), existingRecord2()), res)
                }
                testContext.completeNow()
            }
    }
}
