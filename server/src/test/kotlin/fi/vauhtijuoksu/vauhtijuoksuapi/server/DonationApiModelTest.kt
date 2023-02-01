package fi.vauhtijuoksu.vauhtijuoksuapi.server

import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.donation.DonationApiModel
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID

class DonationApiModelTest {
    private val uuid = UUID.randomUUID()

    // Slight variations in input, within ISO 8601
    private val date = "2021-09-21T15:05:47Z"

    // Accept whatever Date outputs by default, as it's compliant,
    // and we don't care about the exact format further than that
    private val dateUtc = "2021-09-21T15:05:47.000+00:00"

    private val name = "jsolth"
    private val message = "ota tää :D \uD83E\uDD14\uD83E\uDD14"
    private val amount: Float = 10F

    private lateinit var expectedDonation: JsonObject

    @BeforeEach
    fun setup() {
        expectedDonation = JsonObject()
            .put("id", uuid.toString())
            .put("timestamp", dateUtc)
            .put("name", name)
            .put("message", message)
            .put("amount", 10F)
            .put("read", true)
            .put("external_id", "an id")
            .put("incentives", JsonArray())
    }

    @Test
    fun testDonationSerialization() {
        val donation = DonationApiModel(
            uuid,
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(date))),
            name,
            message,
            amount,
            true,
            "an id",
            listOf(),
        )

        val donationAsJson = donation.toJson()
        assertEquals(expectedDonation, donationAsJson)
    }

    @Test
    fun testDonationWithoutMessage() {
        expectedDonation.remove("message")
        val donation = DonationApiModel(
            uuid,
            Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(date))),
            name,
            null,
            amount,
            true,
            "an id",
            listOf(),
        )

        val donationAsJson = donation.toJson()

        assertEquals(expectedDonation, donationAsJson)
    }
}
