package fi.vauhtijuoksu.vauhtijuoksuapi.testdata

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Charity
import java.util.UUID

class TestCharity private constructor() {
    companion object {
        val charity1 = Charity(
            UUID.randomUUID(),
            "Norppien suojelu",
            "https://example.com/norppa",
            10000,
            "norppa.png",
        )

        val charity2 = Charity(
            UUID.randomUUID(),
            "Saimaan suojelu",
            null,
            null,
            null,
        )

        val charity3 = Charity(
            UUID.randomUUID(),
            "Kissojen kerho",
            null,
            500,
            null,
        )
    }
}
