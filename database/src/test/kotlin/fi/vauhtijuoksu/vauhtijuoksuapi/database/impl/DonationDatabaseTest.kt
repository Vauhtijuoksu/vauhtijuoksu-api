package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.Injector
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestDonation.Companion.donation1
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestDonation.Companion.donation2
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestDonation.Companion.donation3
import java.util.UUID

class DonationDatabaseTest : VauhtijuoksuDatabaseTest<Donation>() {
    override fun insertStatement(data: List<Donation>): String {
        fun valuesStringForDonation(donation: Donation): String {
            @Suppress("MaxLineLength")
            return "('${donation.id}', '${donation.name}', '${donation.message}', '${df.format(donation.timestamp)}','${donation.amount}', '${donation.read}')"
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
}
