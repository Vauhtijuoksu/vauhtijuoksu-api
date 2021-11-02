package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.Injector
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestDonation.Companion.donation1
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestDonation.Companion.donation2
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestDonation.Companion.donation3
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

class DonationDatabaseTest : VauhtijuoksuDatabaseTest<Donation>() {
    override fun insertStatement(data: List<Donation>): String {
        fun valuesStringForDonation(donation: Donation): String {
            @Suppress("MaxLineLength")
            return "('${donation.id}', '${donation.name}', '${donation.message}', '${df.format(donation.timestamp)}','${donation.amount}', '${donation.read}', ${donation.externalId?.run { "'${donation.externalId}'" } ?: "NULL"})"
        }

        var statement = "INSERT INTO donations VALUES "
        for (donation in data) {
            statement += "${valuesStringForDonation(donation)},"
        }
        return statement.trim(',')
    }

    override fun existingRecord1(): Donation {
        return donation1
    }

    override fun existingRecord2(): Donation {
        return donation2
    }

    override fun newRecord(): Donation {
        return donation3
    }

    override fun tableName(): String {
        return "donations"
    }

    override fun copyWithId(oldRecord: Donation, newId: UUID): Donation {
        return oldRecord.copy(id = newId)
    }

    override fun getDatabase(injector: Injector): VauhtijuoksuDatabase<Donation> {
        return injector.getInstance(DonationDatabase::class.java)
    }

    @Test
    fun testUpdate(testContext: VertxTestContext) {
        val newDonation = donation1.copy(read = true, message = null)
        db.update(donation1.copy(read = true, message = null))
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(res, newDonation)
                }
            }.compose { db.getAll() }
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(listOf(newDonation, donation2), res)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testUpdatingNonExistingRecord(testContext: VertxTestContext) {
        db.update(donation3)
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertNull(res)
                }
            }
            .compose { db.getAll() }
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(listOf(donation1, donation2), res)
                }
                testContext.completeNow()
            }
    }
}
