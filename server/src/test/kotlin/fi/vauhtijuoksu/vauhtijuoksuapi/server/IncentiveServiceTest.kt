package fi.vauhtijuoksu.vauhtijuoksuapi.server

import fi.vauhtijuoksu.vauhtijuoksuapi.MockitoUtils.Companion.any
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.GeneratedIncentiveCodeDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.VauhtijuoksuException
import fi.vauhtijuoksu.vauhtijuoksuapi.models.ChosenIncentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GeneratedIncentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.IncentiveCode
import fi.vauhtijuoksu.vauhtijuoksuapi.models.IncentiveType
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives.IncentiveService
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives.IncentiveStatus
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives.IncentiveWithStatuses
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives.MilestoneIncentiveStatus
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives.MilestoneStatus
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives.OptionIncentiveStatus
import fi.vauhtijuoksu.vauhtijuoksuapi.verifyAndCompleteTest
import io.vertx.core.Future
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@ExtendWith(VertxExtension::class)
class IncentiveServiceTest {
    private lateinit var incentiveService: IncentiveService

    @Mock
    private lateinit var incentiveDb: VauhtijuoksuDatabase<Incentive>

    @Mock
    private lateinit var incentiveCodeDb: GeneratedIncentiveCodeDatabase

    @Mock
    private lateinit var donationDb: VauhtijuoksuDatabase<Donation>

    private val ts = "2021-09-21T16:05:47-00:00"
    private val milestoneIncentive = Incentive(
        UUID.randomUUID(),
        null,
        "Slotti syö nuubels",
        OffsetDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(ts)), ZoneId.of("Z")),
        IncentiveType.MILESTONE,
        "Noobelit on tulisia",
        listOf(100, 200),
        null,
        null
    )

    private val incentiveCode = IncentiveCode.random()
    private val milestoneGeneratedIncentive = GeneratedIncentive(
        incentiveCode,
        listOf(
            ChosenIncentive(
                milestoneIncentive.id,
                null,
            )
        )
    )

    private val donationWithIncentiveCode = Donation(
        UUID.randomUUID(),
        Date.from(Instant.now()),
        "Hluposti",
        "Ota tää: $incentiveCode",
        60.0F,
        true,
        null,
    )

    private val optionIncentive = Incentive(
        UUID.randomUUID(),
        null,
        "Slotti syö nuubels",
        OffsetDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(ts)), ZoneId.of("Z")),
        IncentiveType.OPTION,
        "Noobelit on tulisia",
        null,
        listOf("kissa", "koira"),
        null
    )

    private val optionGeneratedIncentive = GeneratedIncentive(
        incentiveCode,
        listOf(
            ChosenIncentive(
                optionIncentive.id,
                "kissa",
            )
        )
    )

    private val openIncentive = Incentive(
        UUID.randomUUID(),
        null,
        "Slotti syö nuubels",
        OffsetDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(ts)), ZoneId.of("Z")),
        IncentiveType.OPEN,
        "Noobelit on tulisia",
        null,
        null,
        10
    )

    private val openGeneratedIncentive1 = GeneratedIncentive(
        incentiveCode,
        listOf(
            ChosenIncentive(
                openIncentive.id,
                "kissa",
            )
        )
    )

    private val openGeneratedIncentive2 = GeneratedIncentive(
        IncentiveCode.random(),
        listOf(
            ChosenIncentive(
                openIncentive.id,
                "koira",
            )
        )
    )

    @BeforeEach
    fun setup() {
        incentiveService = IncentiveService(
            incentiveDb,
            incentiveCodeDb,
            donationDb,
        )
    }

    @Test
    fun `getIncentive returns a future failed with an instance of VauhtijuoksuException when there are no incentives`(
        testContext: VertxTestContext
    ) {
        `when`(incentiveDb.getById(any())).thenReturn(Future.succeededFuture(null))
        incentiveService.getIncentive(UUID.randomUUID())
            .onSuccess {
                testContext.completeNow()
            }
            .onFailure {
                testContext.verify {
                    assertTrue(it is VauhtijuoksuException)
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `a milestone incentive shows incomplete status when there are no codes`(testContext: VertxTestContext) {
        `when`(incentiveDb.getById(any())).thenReturn(Future.succeededFuture(milestoneIncentive))
        `when`(incentiveCodeDb.getAll()).thenReturn(Future.succeededFuture(listOf()))
        `when`(donationDb.getAll()).thenReturn(Future.succeededFuture(listOf(donationWithIncentiveCode)))
        incentiveService.getIncentive(UUID.randomUUID())
            .verifyAndCompleteTest(testContext) {
                assertEquals(milestoneIncentive, it.incentive)
                assertEquals(0.0, it.total)
                assertEquals(
                    listOf(
                        MilestoneIncentiveStatus(
                            MilestoneStatus.INCOMPLETE,
                            100,
                        ),
                        MilestoneIncentiveStatus(
                            MilestoneStatus.INCOMPLETE,
                            200,
                        )
                    ),
                    it.statuses
                )
            }
    }

    @Test
    fun `a milestone incentive shows status when there are no donations with codes`(testContext: VertxTestContext) {
        `when`(incentiveDb.getById(any())).thenReturn(Future.succeededFuture(milestoneIncentive))
        `when`(incentiveCodeDb.getAll()).thenReturn(Future.succeededFuture(listOf(milestoneGeneratedIncentive)))
        `when`(donationDb.getAll()).thenReturn(
            Future.succeededFuture(listOf(donationWithIncentiveCode.copy(message = "No code")))
        )
        incentiveService.getIncentive(UUID.randomUUID())
            .verifyAndCompleteTest(testContext) {
                assertEquals(milestoneIncentive, it.incentive)
                assertEquals(0.0, it.total)
                assertEquals(
                    listOf(
                        MilestoneIncentiveStatus(
                            MilestoneStatus.INCOMPLETE,
                            100,
                        ),
                        MilestoneIncentiveStatus(
                            MilestoneStatus.INCOMPLETE,
                            200,
                        )
                    ),
                    it.statuses
                )
            }
    }

    @Test
    fun `an option incentive shows 0 statuses for options when there are no codes`(testContext: VertxTestContext) {
        `when`(incentiveDb.getById(any())).thenReturn(Future.succeededFuture(optionIncentive))
        `when`(incentiveCodeDb.getAll()).thenReturn(Future.succeededFuture(listOf()))
        `when`(donationDb.getAll()).thenReturn(Future.succeededFuture(listOf()))
        incentiveService.getIncentive(UUID.randomUUID())
            .verifyAndCompleteTest(testContext) {
                assertEquals(optionIncentive, it.incentive)
                assertEquals(0.0, it.total)
                assertEquals(
                    listOf(
                        OptionIncentiveStatus(
                            "kissa",
                            0.0,
                        ),
                        OptionIncentiveStatus(
                            "koira",
                            0.0,
                        )
                    ),
                    it.statuses
                )
            }
    }

    @Test
    fun `an open incentive shows no statuses when there are no codes`(testContext: VertxTestContext) {
        `when`(incentiveDb.getById(any())).thenReturn(Future.succeededFuture(openIncentive))
        `when`(incentiveCodeDb.getAll()).thenReturn(Future.succeededFuture(listOf()))
        `when`(donationDb.getAll()).thenReturn(Future.succeededFuture(listOf()))
        incentiveService.getIncentive(UUID.randomUUID())
            .verifyAndCompleteTest(testContext) {
                assertEquals(openIncentive, it.incentive)
                assertEquals(0.0, it.total)
                assertEquals(
                    listOf<IncentiveStatus>(),
                    it.statuses
                )
            }
    }

    @Test
    fun `a milestone incentive status changes to complete when there are enough donations`(testContext: VertxTestContext) {
        `when`(incentiveDb.getById(milestoneIncentive.id)).thenReturn(Future.succeededFuture(milestoneIncentive))
        `when`(incentiveCodeDb.getAll()).thenReturn(Future.succeededFuture(listOf(milestoneGeneratedIncentive)))
        `when`(donationDb.getAll()).thenReturn(
            Future.succeededFuture(
                listOf(
                    donationWithIncentiveCode.copy(amount = 40.0f),
                    donationWithIncentiveCode.copy(id = UUID.randomUUID())
                )
            )
        )

        incentiveService.getIncentive(milestoneIncentive.id)
            .verifyAndCompleteTest(testContext) {
                assertEquals(milestoneIncentive, it.incentive)
                assertEquals(100.0, it.total)
                assertEquals(
                    listOf(
                        MilestoneIncentiveStatus(
                            MilestoneStatus.COMPLETED,
                            100,
                        ),
                        MilestoneIncentiveStatus(
                            MilestoneStatus.INCOMPLETE,
                            200,
                        )
                    ),
                    it.statuses
                )
            }
    }

    @Test
    fun `an option incentive shows correct statuses when there are donations`(testContext: VertxTestContext) {
        `when`(incentiveDb.getById(optionIncentive.id)).thenReturn(Future.succeededFuture(optionIncentive))
        `when`(incentiveCodeDb.getAll()).thenReturn(Future.succeededFuture(listOf(optionGeneratedIncentive)))
        `when`(donationDb.getAll()).thenReturn(
            Future.succeededFuture(
                listOf(
                    donationWithIncentiveCode,
                    donationWithIncentiveCode.copy(id = UUID.randomUUID())
                )
            )
        )

        incentiveService.getIncentive(optionIncentive.id)
            .verifyAndCompleteTest(testContext) {
                assertEquals(optionIncentive, it.incentive)
                assertEquals(120.0, it.total)
                assertEquals(
                    listOf(
                        OptionIncentiveStatus(
                            "kissa",
                            120.0,
                        ),
                        OptionIncentiveStatus(
                            "koira",
                            0.0,
                        )
                    ),
                    it.statuses
                )
            }
    }

    @Test
    fun `an open incentive shows all submitted options with donations`(testContext: VertxTestContext) {
        `when`(incentiveDb.getById(openIncentive.id)).thenReturn(Future.succeededFuture(openIncentive))
        `when`(incentiveCodeDb.getAll()).thenReturn(
            Future.succeededFuture(
                listOf(
                    openGeneratedIncentive1,
                    openGeneratedIncentive2
                )
            )
        )
        `when`(donationDb.getAll()).thenReturn(
            Future.succeededFuture(
                listOf(
                    donationWithIncentiveCode,
                    donationWithIncentiveCode.copy(id = UUID.randomUUID())
                )
            )
        )

        incentiveService.getIncentive(openIncentive.id)
            .verifyAndCompleteTest(testContext) {
                assertEquals(openIncentive, it.incentive)
                assertEquals(120.0, it.total)
                assertEquals(
                    listOf(
                        OptionIncentiveStatus(
                            "kissa",
                            120.0,
                        )
                    ),
                    it.statuses
                )
            }
    }

    @Test
    fun `an open incentive groups options from multiple codes`(testContext: VertxTestContext) {
        val incentiveCode2 = IncentiveCode.random()
        `when`(incentiveDb.getById(openIncentive.id)).thenReturn(Future.succeededFuture(openIncentive))
        `when`(incentiveCodeDb.getAll()).thenReturn(
            Future.succeededFuture(
                listOf(
                    openGeneratedIncentive1,
                    openGeneratedIncentive1.copy(generatedCode = incentiveCode2)
                )
            )
        )
        `when`(donationDb.getAll()).thenReturn(
            Future.succeededFuture(
                listOf(
                    donationWithIncentiveCode,
                    donationWithIncentiveCode.copy(id = UUID.randomUUID(), message = "Monies for cats $incentiveCode2")
                )
            )
        )

        incentiveService.getIncentive(openIncentive.id)
            .verifyAndCompleteTest(testContext) {
                assertEquals(openIncentive, it.incentive)
                assertEquals(120.0, it.total)
                assertEquals(
                    listOf(
                        OptionIncentiveStatus(
                            "kissa",
                            120.0,
                        )
                    ),
                    it.statuses
                )
            }
    }

    @Test
    fun `donations are shared between multiple codes in donation message`(testContext: VertxTestContext) {
        `when`(incentiveDb.getById(any())).thenReturn(Future.succeededFuture(milestoneIncentive))
        `when`(incentiveCodeDb.getAll()).thenReturn(Future.succeededFuture(listOf(milestoneGeneratedIncentive)))
        `when`(donationDb.getAll()).thenReturn(
            Future.succeededFuture(
                listOf(
                    donationWithIncentiveCode.copy(message = "two codes: $incentiveCode and {#Vj2020}"),
                )
            )
        )

        incentiveService.getIncentive(milestoneIncentive.id)
            .verifyAndCompleteTest(testContext) {
                assertEquals(30.0, it.total)
            }
    }

    @Test
    fun `donations are shared between multiple incentives in incentive code`(testContext: VertxTestContext) {
        `when`(incentiveDb.getById(any())).thenReturn(Future.succeededFuture(milestoneIncentive))
        `when`(incentiveCodeDb.getAll()).thenReturn(
            Future.succeededFuture(
                listOf(
                    milestoneGeneratedIncentive.copy(
                        chosenIncentives = listOf(
                            ChosenIncentive(
                                UUID.randomUUID(),
                                null,
                            ),
                            ChosenIncentive(
                                milestoneIncentive.id,
                                null,
                            )
                        )
                    )
                )
            )
        )
        `when`(donationDb.getAll()).thenReturn(Future.succeededFuture(listOf(donationWithIncentiveCode)))

        incentiveService.getIncentive(milestoneIncentive.id)
            .verifyAndCompleteTest(testContext) {
                assertEquals(30.0, it.total)
            }
    }

    @Test
    fun `multiple options for one incentive with one code works`(testContext: VertxTestContext) {
        `when`(incentiveDb.getById(any())).thenReturn(Future.succeededFuture(optionIncentive))
        `when`(incentiveCodeDb.getAll()).thenReturn(
            Future.succeededFuture(
                listOf(
                    optionGeneratedIncentive.copy(
                        chosenIncentives = listOf(
                            ChosenIncentive(optionIncentive.id, "kissa"),
                            ChosenIncentive(optionIncentive.id, "koira"),
                        )
                    )
                )
            )
        )
        `when`(donationDb.getAll()).thenReturn(Future.succeededFuture(listOf(donationWithIncentiveCode)))
        incentiveService.getIncentive(optionIncentive.id)
            .verifyAndCompleteTest(testContext) {
                assertEquals(60.0, it.total)
                assertEquals(
                    listOf(
                        OptionIncentiveStatus(
                            "kissa",
                            30.0,
                        ),
                        OptionIncentiveStatus(
                            "koira",
                            30.0,
                        ),
                    ),
                    it.statuses
                )
            }
    }

    @Test
    fun `multiple open choices for one incentive with one code works`(testContext: VertxTestContext) {
        `when`(incentiveDb.getById(any())).thenReturn(Future.succeededFuture(openIncentive))
        `when`(incentiveCodeDb.getAll()).thenReturn(
            Future.succeededFuture(
                listOf(
                    openGeneratedIncentive1.copy(
                        chosenIncentives = listOf(
                            ChosenIncentive(openIncentive.id, "kissa"),
                            ChosenIncentive(openIncentive.id, "koira"),
                        )
                    )
                )
            )
        )
        `when`(donationDb.getAll()).thenReturn(Future.succeededFuture(listOf(donationWithIncentiveCode)))
        incentiveService.getIncentive(openIncentive.id)
            .verifyAndCompleteTest(testContext) {
                assertEquals(60.0, it.total)
                assertEquals(
                    listOf(
                        OptionIncentiveStatus(
                            "kissa",
                            30.0,
                        ),
                        OptionIncentiveStatus(
                            "koira",
                            30.0,
                        ),
                    ),
                    it.statuses
                )
            }
    }

    @Test
    fun `getIncentives returns empty list when there are no incentives`(testContext: VertxTestContext) {
        `when`(incentiveDb.getAll()).thenReturn(Future.succeededFuture(listOf()))
        `when`(incentiveCodeDb.getAll()).thenReturn(Future.succeededFuture(listOf()))
        `when`(donationDb.getAll()).thenReturn(Future.succeededFuture(listOf()))
        incentiveService.getIncentives()
            .onFailure(testContext::failNow)
            .onSuccess {
                testContext.verify {
                    assertTrue(it.isEmpty())
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `getIncentives returns incentives when there are no codes nor donations`(testContext: VertxTestContext) {
        `when`(incentiveDb.getAll()).thenReturn(
            Future.succeededFuture(
                listOf(
                    milestoneIncentive,
                    optionIncentive,
                )
            )
        )
        `when`(incentiveCodeDb.getAll()).thenReturn(Future.succeededFuture(listOf()))
        `when`(donationDb.getAll()).thenReturn(Future.succeededFuture(listOf()))
        incentiveService.getIncentives()
            .onFailure(testContext::failNow)
            .onSuccess {
                testContext.verify {
                    assertEquals(
                        listOf(
                            IncentiveWithStatuses(
                                milestoneIncentive,
                                0.0,
                                listOf(
                                    MilestoneIncentiveStatus(
                                        MilestoneStatus.INCOMPLETE,
                                        100,
                                    ),
                                    MilestoneIncentiveStatus(
                                        MilestoneStatus.INCOMPLETE,
                                        200,
                                    ),
                                ),
                            ),
                            IncentiveWithStatuses(
                                optionIncentive,
                                0.0,
                                listOf(
                                    OptionIncentiveStatus(
                                        "kissa",
                                        0.0,
                                    ),
                                    OptionIncentiveStatus(
                                        "koira",
                                        0.0,
                                    ),
                                ),
                            )
                        ),
                        it
                    )
                }
                testContext.completeNow()
            }
    }
}
