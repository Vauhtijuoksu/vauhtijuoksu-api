package fi.vauhtijuoksu.vauhtijuoksuapi.testdata

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class TestTimer private constructor() {
    companion object {
        private const val start = "2021-09-21T16:05:47-00:00"
        private const val end = "2021-09-22T16:05:47-00:00"

        val timer1 = Timer(
            UUID.randomUUID(),
            OffsetDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(start)), ZoneId.of("Z")),
            OffsetDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(end)), ZoneId.of("Z")),
            "some timer",
        )

        val timer2 = Timer(
            UUID.randomUUID(),
            OffsetDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(start)), ZoneId.of("Z")).minusDays(1),
            OffsetDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(end)), ZoneId.of("Z")).plusDays(1),
            "another timer",
        )

        val timer3 = Timer(
            UUID.randomUUID(),
            OffsetDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(start)), ZoneId.of("Z")).minusHours(1),
            OffsetDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(end)), ZoneId.of("Z")).plusDays(2),
            "yet another timer",
        )
    }
}
