package fi.vauhtijuoksu.vauhtijuoksuapi.testdata

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID

class TestDonation private constructor() {
    companion object {

        private val date1 = Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2021-09-21T15:05:47Z")))
        private val date2 = Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2021-09-22T13:15:47Z")))
        private val date3 = Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse("1960-09-21T05:05:47Z")))

        val donation1 = Donation(
            UUID.randomUUID(),
            date1,
            "jsolth",
            "ota tää :D 🤔🤔",
            10F,
            false
        )
        val donation2 = Donation(
            UUID.randomUUID(),
            date2,
            "luuranki",
            "mee tänne: töihin :))))))))",
            5F,
            true
        )
        val donation3 = Donation(
            UUID.randomUUID(),
            date3,
            "töi henkilö",
            "nythän on tiistai ja pitäisi olla töissä",
            5000F,
            false,
        )
    }
}
