package fi.vauhtijuoksu.vauhtijuoksuapi.server

import arrow.core.left
import arrow.core.right
import com.google.inject.AbstractModule
import com.google.inject.Module
import fi.vauhtijuoksu.vauhtijuoksuapi.MockitoUtils.Companion.any
import fi.vauhtijuoksu.vauhtijuoksuapi.models.PlayerInfo
import fi.vauhtijuoksu.vauhtijuoksuapi.server.discord.DiscordClient
import fi.vauhtijuoksu.vauhtijuoksuapi.server.discord.DiscordError
import fi.vauhtijuoksu.vauhtijuoksuapi.server.discord.DiscordUser
import io.kotest.matchers.shouldBe
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials
import io.vertx.ext.web.client.WebClientSession
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.test.runTest
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class AuthTest : ServerTestBase() {
    lateinit var server: MockOAuth2Server

    @Mock
    protected lateinit var discordClient: DiscordClient

    @BeforeAll
    fun before() {
        server = MockOAuth2Server()
        server.start()
    }

    @AfterAll
    fun after() {
        server.shutdown()
    }

    override fun oauthServerPort(): Int {
        return server.url("/").port
    }

    override fun modules(serverPort: Int): List<Module> {
        return super.modules(serverPort) + listOf(object : AbstractModule() {
            override fun configure() {
                bind(DiscordClient::class.java).toInstance(discordClient)
            }
        })
    }

    @Test
    fun `htpasswd users are admins`() = runTest {
        `when`(playerInfoDb.get()).thenReturn(
            Future.succeededFuture(
                PlayerInfo("ota tää"),
            ),
        )
        `when`(playerInfoDb.save(any())).thenReturn(Future.succeededFuture())

        val res = client.patch("/player-info")
            .authentication(UsernamePasswordCredentials(username, password))
            .sendJson(JsonObject().put("message", "ota tää"))
            .coAwait()
        res.statusCode() shouldBe 200
        verify(playerInfoDb).save(any())
    }

    @Test
    fun `Admin discord users can patch`() = runTest {
        `when`(playerInfoDb.get()).thenReturn(
            Future.succeededFuture(
                PlayerInfo("ota tää"),
            ),
        )
        `when`(playerInfoDb.save(any())).thenReturn(Future.succeededFuture())

        val sessionClient = WebClientSession.create(client)
        val login = sessionClient.get("/login")
            .followRedirects(true)
            .send()
            .coAwait()
        login.statusCode() shouldBe 200

        `when`(discordClient.getUser(any())).thenReturn(DiscordUser("Nörtti", true).right())

        val patch = sessionClient.patch("/player-info")
            .sendJson(JsonObject().put("message", "ota tää"))
            .coAwait()
        patch.statusCode() shouldBe 200
        verify(playerInfoDb).save(any())
    }

    @Test
    fun `Discord users in Vauhtijuoksu server are not admins`() = runTest {
        val sessionClient = WebClientSession.create(client)
        val login = sessionClient.get("/login")
            .followRedirects(true)
            .send()
            .coAwait()
        login.statusCode() shouldBe 200

        `when`(discordClient.getUser(any())).thenReturn(DiscordUser("Nörtti", false).right())

        val patch = sessionClient.patch("/player-info")
            .sendJson(JsonObject().put("message", "ota tää"))
            .coAwait()
        patch.statusCode() shouldBe 403
    }

    @Test
    fun `Discord users that are not on the Vauhtijuoksu server are not admins`() = runTest {
        val sessionClient = WebClientSession.create(client)
        val login = sessionClient.get("/login")
            .followRedirects(true)
            .send()
            .coAwait()
        login.statusCode() shouldBe 200

        `when`(discordClient.getUser(any())).thenReturn(DiscordError.NOT_A_MEMBER.left())

        val patch = sessionClient.patch("/player-info")
            .sendJson(JsonObject().put("message", "ota tää"))
            .coAwait()
        patch.statusCode() shouldBe 403
    }
}
