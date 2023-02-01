package fi.vauhtijuoksu.vauhtijuoksuapi.testdata

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.IncentiveType
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class TestIncentive private constructor() {
    companion object {
        private const val end1 = "2021-09-21T16:05:47-00:00"
        private const val end2 = "2021-09-22T16:05:47-00:00"

        val incentive1 = Incentive(
            UUID.randomUUID(),
            null,
            "Slotti syö nuubels",
            OffsetDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(end1)), ZoneId.of("Z")),
            IncentiveType.MILESTONE,
            "Noobelit on tulisia",
            listOf(100),
            null,
            null,
        )
        val incentive2 = Incentive(
            UUID.randomUUID(),
            null,
            "Glukoosin lempisokeri",
            OffsetDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(end2)), ZoneId.of("Z")),
            IncentiveType.OPEN,
            "Ehdota uutta sokerinimeä",
            null,
            null,
            100,
        )
        val incentive3 = Incentive(
            UUID.randomUUID(),
            null,
            "Slotin lempipeli",
            OffsetDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(end1)), ZoneId.of("Z")),
            IncentiveType.OPTION,
            "Äänestä slotin lempipeli",
            null,
            listOf("Kingdom Hearts 1", "Kingdom Hearts 2", "Kingdom Hearts 3"),
            null,
        )
    }
}
