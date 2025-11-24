package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.TypeLiteral
import com.google.inject.util.Modules
import com.google.inject.util.Providers
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.GeneratedIncentiveCodeDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.SingletonDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.impl.StreamMetadataDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Participant
import fi.vauhtijuoksu.vauhtijuoksuapi.models.PlayerInfo
import fi.vauhtijuoksu.vauhtijuoksuapi.models.StreamMetadata
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.DiscordClientConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.OAuthConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.RedisConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.ServerConfiguration
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.ServerSocket
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@ExtendWith(MockitoExtension::class)
@ExtendWith(VertxExtension::class)
open class ServerTestBase {
    private lateinit var vertx: Vertx
    private lateinit var server: Server
    protected lateinit var client: WebClient

    @Mock
    protected lateinit var gameDataDb: VauhtijuoksuDatabase<GameData>

    @Mock
    protected lateinit var donationDb: VauhtijuoksuDatabase<Donation>

    @Mock
    protected lateinit var timerDb: VauhtijuoksuDatabase<Timer>

    @Mock
    protected lateinit var streamMetadataDb: SingletonDatabase<StreamMetadata>

    @Mock
    protected lateinit var playerInfoDb: SingletonDatabase<PlayerInfo>

    @Mock
    protected lateinit var incentiveDatabase: VauhtijuoksuDatabase<Incentive>

    @Mock
    protected lateinit var generatedIncentiveCodeDatabase: GeneratedIncentiveCodeDatabase

    @Mock
    protected lateinit var streamMetadataDatabase: StreamMetadataDatabase

    @Mock
    protected lateinit var participantDatabase: VauhtijuoksuDatabase<Participant>

    protected val clock: Clock = Clock.fixed(Instant.now(), ZoneId.of("Europe/Helsinki"))

    @TempDir
    lateinit var tmpDir: File
    lateinit var htpasswdFile: String

    protected val username = "vauhtijuoksu"
    protected val password = "vauhtijuoksu"

    protected val corsHeaderUrl = "https://(\\w+[.])?vauhtijuoksu.fi"
    protected val allowedOrigin = "https://newapi.vauhtijuoksu.fi"

    private fun getFreePort(): Int {
        val sock = ServerSocket(0)
        val port = sock.localPort
        sock.close()
        return port
    }

    /**
     * Used in AuthTest
     */
    open fun oauthServerPort() = 0

    fun oAuthConfiguration(serverPort: Int): OAuthConfiguration =
        OAuthConfiguration(
            "id",
            "secret",
            "http://localhost:$serverPort/callback",
            "http://localhost:${oauthServerPort()}",
            scopes = listOf("openid"), // The mock OAuth server requires this scope
        )

    open fun modules(serverPort: Int): List<Module> = listOf(
        Modules.override(ApiModule()).with(object : AbstractModule() {
            override fun configure() {
                bind(Clock::class.java).toInstance(clock)
            }
        }),
        AuthModule(),
        object : AbstractModule() {
            override fun configure() {
                bind(object : TypeLiteral<VauhtijuoksuDatabase<GameData>>() {}).toInstance(gameDataDb)
                bind(object : TypeLiteral<VauhtijuoksuDatabase<Donation>>() {}).toInstance(donationDb)
                bind(object : TypeLiteral<VauhtijuoksuDatabase<Timer>>() {}).toInstance(timerDb)
                bind(object : TypeLiteral<SingletonDatabase<StreamMetadata>>() {}).toInstance(streamMetadataDb)
                bind(object : TypeLiteral<SingletonDatabase<PlayerInfo>>() {}).toInstance(playerInfoDb)
                bind(object : TypeLiteral<VauhtijuoksuDatabase<Incentive>>() {}).toInstance(incentiveDatabase)
                bind(GeneratedIncentiveCodeDatabase::class.java).toInstance(generatedIncentiveCodeDatabase)
                bind(object : TypeLiteral<StreamMetadataDatabase>() {}).toInstance(streamMetadataDatabase)
                bind(object : TypeLiteral<VauhtijuoksuDatabase<Participant>>() {}).toInstance(participantDatabase)
                bind(ServerConfiguration::class.java).toInstance(
                    ServerConfiguration(
                        serverPort,
                        htpasswdFile,
                        listOf(corsHeaderUrl),
                        false,
                    ),
                )
                bind(RedisConfiguration::class.java).toProvider(Providers.of(null))
                bind(OAuthConfiguration::class.java).toInstance(oAuthConfiguration(serverPort))
                bind(DiscordClientConfiguration::class.java).toInstance(
                    DiscordClientConfiguration(
                        vauhtijuoksuServerId = "serverId",
                        adminRoleId = "adminRoleId",
                    ),
                )
            }
        },
    )

    open fun injector(modules: List<Module>): Injector = Guice.createInjector(modules)

    @BeforeEach
    fun beforeEach(testContext: VertxTestContext) {
        htpasswdFile = "${tmpDir.path}/.htpasswd"
        val writer = BufferedWriter(FileWriter(File(htpasswdFile)))
        // Pre-generated credentials vauhtijuoksu / vauhtijuoksu
        writer.write("vauhtijuoksu:{SHA}Iih8iFrD8jPkj1eYEw6tJmTbHrg=")
        writer.close()

        val serverPort = getFreePort()
        val injector = Guice.createInjector(modules(serverPort))

        vertx = injector.getInstance(Vertx::class.java)
        server = injector.getInstance(Server::class.java)
        server.start()
        client = WebClient.create(vertx, WebClientOptions().setDefaultPort(serverPort))
        var runs = 0
        vertx.setPeriodic(0, 5) { timerId ->
            client.request(HttpMethod.OPTIONS, "/")
                .send()
                .onSuccess {
                    vertx.cancelTimer(timerId)
                    testContext.completeNow()
                }
                .onFailure {
                    runs += 1
                    if (runs > 10) {
                        testContext.failNow("Server not responding after 10 tries")
                    }
                }
        }
    }

    @AfterEach
    fun afterEach(testContext: VertxTestContext) {
        server.stop()
        client.close()
        vertx.close { testContext.completeNow() }
    }
}
