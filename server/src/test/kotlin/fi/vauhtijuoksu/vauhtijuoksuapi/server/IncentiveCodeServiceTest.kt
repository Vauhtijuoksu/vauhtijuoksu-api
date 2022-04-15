package fi.vauhtijuoksu.vauhtijuoksuapi.server

import fi.vauhtijuoksu.vauhtijuoksuapi.MockitoUtils.Companion.any
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.GeneratedIncentiveCodeDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.VauhtijuoksuException
import fi.vauhtijuoksu.vauhtijuoksuapi.models.ChosenIncentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GeneratedIncentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.IncentiveCode
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentivecodes.IncentiveCodeService
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestIncentive
import io.vertx.core.Future
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@ExtendWith(VertxExtension::class)
class IncentiveCodeServiceTest {
    private lateinit var incentiveCodeService: IncentiveCodeService

    @Mock
    private lateinit var incentiveCodeDatabase: GeneratedIncentiveCodeDatabase

    @Mock
    private lateinit var incentiveDatabase: VauhtijuoksuDatabase<Incentive>

    @BeforeEach
    fun setup() {
        incentiveCodeService = IncentiveCodeService(incentiveCodeDatabase, incentiveDatabase)
        lenient().`when`(incentiveDatabase.getById(milestoneIncentive.id)).thenReturn(
            Future.succeededFuture(milestoneIncentive)
        )
        lenient().`when`(incentiveDatabase.getById(optionIncentive.id)).thenReturn(
            Future.succeededFuture(optionIncentive)
        )
        lenient().`when`(incentiveDatabase.getById(openIncentive.id)).thenReturn(
            Future.succeededFuture(openIncentive)
        )
    }

    private val milestoneIncentive = TestIncentive.incentive1
    private val openIncentive = TestIncentive.incentive2
    private val optionIncentive = TestIncentive.incentive3

    @Test
    fun `the service generates a new code and saves it to db`(testContext: VertxTestContext) {
        `when`(incentiveCodeDatabase.add(any())).thenReturn(Future.succeededFuture())
        incentiveCodeService.generateCode(listOf(ChosenIncentive(milestoneIncentive.id, null)))
            .onFailure(testContext::failNow)
            .onSuccess {
                testContext.verify {
                    verify(incentiveCodeDatabase).add(
                        GeneratedIncentive(
                            IncentiveCode(it.code),
                            listOf(ChosenIncentive(milestoneIncentive.id, null)),
                        )
                    )
                }
                testContext.completeNow()
            }
    }

    @Test
    fun `code generation fails when no incentives are chosen`(testContext: VertxTestContext) {
        incentiveCodeService.generateCode(listOf()).expectToFailWithVauhtijuoksuException(testContext)
    }

    @Test
    fun `code generation fails when unknown incentive is chosen`(testContext: VertxTestContext) {
        `when`(incentiveDatabase.getById(any())).thenReturn(Future.succeededFuture())
        incentiveCodeService.generateCode(listOf(ChosenIncentive(UUID.randomUUID(), null)))
            .expectToFailWithVauhtijuoksuException(testContext)
    }

    @Test
    fun `the service fails if option is given for a milestone incentive`(testContext: VertxTestContext) {
        incentiveCodeService.generateCode(listOf(ChosenIncentive(milestoneIncentive.id, "kissa")))
            .expectToFailWithVauhtijuoksuException(testContext)
    }

    @Test
    fun `the service fails if unknown option is chosen for option incentive`(testContext: VertxTestContext) {
        incentiveCodeService.generateCode(listOf(ChosenIncentive(optionIncentive.id, "Unknown option")))
            .expectToFailWithVauhtijuoksuException(testContext)
    }

    @Test
    fun `the service fails if no option is chosen for option incentive`(testContext: VertxTestContext) {
        incentiveCodeService.generateCode(listOf(ChosenIncentive(optionIncentive.id, null)))
            .expectToFailWithVauhtijuoksuException(testContext)
    }

    @Test
    fun `the service fails if too long option is chosen for open incentive`(testContext: VertxTestContext) {
        incentiveCodeService.generateCode(
            listOf(ChosenIncentive(openIncentive.id, "A".repeat(openIncentive.openCharLimit!! + 1)))
        ).expectToFailWithVauhtijuoksuException(testContext)
    }

    @Test
    fun `the service fails if too no option is chosen for open incentive`(testContext: VertxTestContext) {
        incentiveCodeService.generateCode(listOf(ChosenIncentive(openIncentive.id, null)))
            .expectToFailWithVauhtijuoksuException(testContext)
    }

    private fun <T> Future<T>.expectToFailWithVauhtijuoksuException(testContext: VertxTestContext) {
        this.onSuccess { testContext.failNow("Expected to fail") }.onFailure {
            testContext.verify {
                assertTrue(it is VauhtijuoksuException)
            }
            testContext.completeNow()
        }
    }
}
