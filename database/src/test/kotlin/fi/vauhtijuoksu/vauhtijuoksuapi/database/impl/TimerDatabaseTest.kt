package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.Injector
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class TimerDatabaseTest : VauhtijuoksuDatabaseTest<Timer>() {
    private val timer1 = Timer(
        UUID.randomUUID(),
        OffsetDateTime.ofInstant(
            Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2022-05-05T16:00:00Z")),
            ZoneId.of("Z"),
        ),
        OffsetDateTime.ofInstant(
            Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2022-05-06T16:00:00Z")),
            ZoneId.of("Z"),
        ),
    )

    private val timer2 = Timer(
        UUID.randomUUID(),
        OffsetDateTime.ofInstant(
            Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2022-05-06T16:00:00Z")),
            ZoneId.of("Z"),
        ),
        OffsetDateTime.ofInstant(
            Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2022-05-07T16:00:00Z")),
            ZoneId.of("Z"),
        ),
    )

    private val timer3 = Timer(
        UUID.randomUUID(),
        OffsetDateTime.ofInstant(
            Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2022-05-07T16:00:00Z")),
            ZoneId.of("Z"),
        ),
        OffsetDateTime.ofInstant(
            Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2022-05-08T16:00:00Z")),
            ZoneId.of("Z"),
        ),
    )

    override fun insertStatement(data: List<Timer>): String {
        fun valuesStringForTimer(timer: Timer): String {
            @Suppress("MaxLineLength")
            return "('${timer.id}', '${timer.startTime}', '${timer.endTime}')"
        }

        var statement = "INSERT INTO timers VALUES "
        for (timer in data) {
            statement += "${valuesStringForTimer(timer)},"
        }
        return statement.trim(',')
    }

    override fun existingRecord1(): Timer {
        return timer1
    }

    override fun existingRecord2(): Timer {
        return timer2
    }

    override fun newRecord(): Timer {
        return timer3
    }

    override fun tableName(): String {
        return "timers"
    }

    override fun copyWithId(oldRecord: Timer, newId: UUID): Timer {
        return oldRecord.copy(id = newId)
    }

    override fun getDatabase(injector: Injector): VauhtijuoksuDatabase<Timer> {
        return injector.getInstance(TimerDatabase::class.java)
    }

    @Test
    fun testUpdate(testContext: VertxTestContext) {
        val newTimer = timer1.copy(
            startTime = OffsetDateTime.ofInstant(
                Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2022-06-05T16:00:00Z")),
                ZoneId.of("Z"),
            ),
            endTime = OffsetDateTime.ofInstant(
                Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2022-06-06T16:00:00Z")),
                ZoneId.of("Z"),
            ),
        )
        db.update(
            timer1.copy(
                startTime = OffsetDateTime.ofInstant(
                    Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2022-06-05T16:00:00Z")),
                    ZoneId.of("Z"),
                ),
                endTime = OffsetDateTime.ofInstant(
                    Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2022-06-06T16:00:00Z")),
                    ZoneId.of("Z"),
                ),
            ),
        )
            .compose { db.getAll() }
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    val list1 = mutableListOf(newTimer, timer2)
                    list1.sortBy { it.id }
                    val results = res.toMutableList()
                    results.sortBy { it.id }
                    assertEquals(list1, results)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun testUpdatingNonExistingRecord(testContext: VertxTestContext) {
        db.update(timer3)
            .failOnSuccess(testContext)
            .recoverIfMissingEntity(testContext)
            .compose { db.getAll() }
            .onFailure(testContext::failNow)
            .onSuccess { res ->
                testContext.verify {
                    assertEquals(listOf(timer1, timer2), res)
                }
                testContext.completeNow()
            }
    }
}
