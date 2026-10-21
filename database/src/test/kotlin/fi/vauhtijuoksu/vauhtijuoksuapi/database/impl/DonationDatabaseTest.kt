package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.Injector
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.UserError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestCharity.Companion.charity1
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestCharity.Companion.charity2
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestDonation.Companion.donation1
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestDonation.Companion.donation2
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestDonation.Companion.donation3
import io.vertx.core.Future
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class DonationDatabaseTest : VauhtijuoksuDatabaseTest<Donation>() {
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

    // Donations refer to charities, so they must exist first
    override fun insertExistingRecords(): Future<Unit> {
        val charityDb = injector.getInstance(CharityDatabase::class.java)
        return charityDb.add(charity1)
            .flatMap { charityDb.add(charity2) }
            .flatMap { super.insertExistingRecords() }
    }

    @Test
    fun testAddingDonationWithUnknownCharityIsUserError() = runTest {
        db.add(donation3.copy(charity = UUID.randomUUID()))
            .failOnSuccess()
            .recover {
                assertTrue(it is UserError, "Expected UserError, got $it")
                Future.succeededFuture()
            }
            .coAwait()
    }

    @Test
    fun testUpdate() = runTest {
        val newDonation = donation1.copy(read = true, message = null, charity = charity2.id)
        db.update(newDonation)
            .coAwait()

        db.getAll()
            .coAwait()
            .let { res ->
                assertEquals(listOf(newDonation, donation2), res)
            }
    }

    @Test
    fun testUpdatingNonExistingRecord() = runTest {
        db.update(donation3)
            .failOnSuccess()
            .recoverIfMissingEntity()
            .coAwait()

        db.getAll()
            .coAwait()
            .let { res ->
                assertEquals(listOf(donation1, donation2), res)
            }
    }
}
