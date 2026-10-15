package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.Injector
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.UserError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Charity
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestCharity.Companion.charity1
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestCharity.Companion.charity2
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestCharity.Companion.charity3
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestDonation.Companion.donation1
import io.vertx.core.Future
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class CharityDatabaseTest : VauhtijuoksuDatabaseTest<Charity>() {
    override fun existingRecord1(): Charity {
        return charity1
    }

    override fun existingRecord2(): Charity {
        return charity2
    }

    override fun newRecord(): Charity {
        return charity3
    }

    override fun tableName(): String {
        return "charities"
    }

    override fun copyWithId(oldRecord: Charity, newId: UUID): Charity {
        return oldRecord.copy(id = newId)
    }

    override fun getDatabase(injector: Injector): VauhtijuoksuDatabase<Charity> {
        return injector.getInstance(CharityDatabase::class.java)
    }

    @Test
    fun `a charity with donations can't be deleted`() = runTest {
        val donationDb = injector.getInstance(DonationDatabase::class.java)
        donationDb.add(donation1).coAwait()

        db.delete(charity1.id)
            .failOnSuccess()
            .recover {
                assertTrue(it is UserError, "Expected UserError, got $it")
                Future.succeededFuture()
            }
            .coAwait()
        assertEquals(charity1, db.getById(charity1.id).coAwait())
    }
}
