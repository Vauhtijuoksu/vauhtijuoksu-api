package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.google.inject.AbstractModule
import com.google.inject.Guice
import fi.vauhtijuoksu.vauhtijuoksuapi.database.DatabaseModule
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.GeneratedIncentiveCodeDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.models.ChosenIncentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GeneratedIncentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.IncentiveCode
import fi.vauhtijuoksu.vauhtijuoksuapi.testdata.TestIncentive
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@ExtendWith(VertxExtension::class)
class GeneratedIncentiveDatabaseTest {
    private lateinit var db: GeneratedIncentiveCodeDatabase
    private lateinit var incentiveDb: VauhtijuoksuDatabase<Incentive>

    @Container
    var pg: PostgreSQLContainer<Nothing> =
        PostgreSQLContainer<Nothing>("postgres:10").withDatabaseName("vauhtijuoksu-api")

    @BeforeEach
    fun beforeEach() {
        val injector =
            Guice.createInjector(
                DatabaseModule(),
                object : AbstractModule() {
                    override fun configure() {
                        bind(DatabaseConfiguration::class.java).toInstance(
                            DatabaseConfiguration(
                                pg.host,
                                pg.firstMappedPort,
                                "vauhtijuoksu-api",
                                pg.username,
                                pg.password,
                                6,
                            ),
                        )
                    }
                },
            )

        db = injector.getInstance(GeneratedIncentiveCodeDatabase::class.java)
        incentiveDb = injector.getInstance(IncentiveDatabase::class.java)
    }

    @Test
    fun `multiple codes can be added and fetched`(testContext: VertxTestContext) {
        val incentiveId1 = TestIncentive.incentive1.id
        val incentiveId2 = TestIncentive.incentive2.id

        val generatedIncentive1 =
            GeneratedIncentive(
                IncentiveCode.random(),
                listOf(
                    ChosenIncentive(incentiveId1, null),
                    ChosenIncentive(incentiveId2, null),
                ),
            )
        val generatedIncentive2 =
            GeneratedIncentive(
                IncentiveCode.random(),
                listOf(
                    ChosenIncentive(incentiveId2, "some param"),
                ),
            )
        incentiveDb
            .add(TestIncentive.incentive1)
            .compose { incentiveDb.add(TestIncentive.incentive2) }
            .compose { db.add(generatedIncentive1) }
            .compose { db.add(generatedIncentive2) }
            .compose { db.getAll() }
            .onSuccess {
                testContext.verify {
                    assertEquals(listOf(generatedIncentive1, generatedIncentive2), it)
                }
                testContext.completeNow()
            }.onFailure(testContext::failNow)
    }
}
