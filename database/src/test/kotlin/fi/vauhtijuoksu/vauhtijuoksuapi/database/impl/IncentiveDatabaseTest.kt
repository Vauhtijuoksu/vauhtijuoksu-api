package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.Injector
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestIncentive
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class IncentiveDatabaseTest : VauhtijuoksuDatabaseTest<Incentive>() {
    override fun insertStatement(data: List<Incentive>): String {
        fun valuesStringForIncentive(incentive: Incentive): String {
            fun intArrayOrNull(value: List<Int>?): String {
                return value?.joinToString(",", "'{", "}'") ?: "NULL"
            }

            fun arrayOrNull(value: List<Any>?): String {
                return value?.stream()?.map { "$it" }?.toList()?.joinToString(",", "'{", "}'") ?: "NULL"
            }

            fun valueOrNULL(value: Any?): String? {
                return if (value != null) {
                    "'$value'"
                } else {
                    null
                }
            }

            return """(
                '${incentive.id}',
                ${valueOrNULL(incentive.gameId)},
                '${incentive.title}',
                '${incentive.endTime}',
                '${incentive.type}',
                '${incentive.info}',
                ${intArrayOrNull(incentive.milestones)},
                ${arrayOrNull(incentive.optionParameters)},
                ${
                if (incentive.openCharLimit == null){
                    "NULL"
                } else {
                    "${incentive.openCharLimit}"
                }
            }
                )"""
        }

        var statement = "INSERT INTO incentives VALUES "
        for (incentive in data) {
            statement += "${valuesStringForIncentive(incentive)},"
        }
        return statement.trim(',')
    }

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
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(expectedIncentive, res)
                }
            }.compose { db.getAll() }
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
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    Assertions.assertNull(res)
                }
            }
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
