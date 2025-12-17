package fi.vauhtijuoksu.vauhtijuoksuapi.server.discord

import arrow.core.Either
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.DiscordClientConfiguration
import io.kotest.matchers.shouldBe
import io.vertx.core.Vertx
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

private fun <A, B> Either<A, B>.verifyRight(verifications: (B) -> Unit) {
    fold({
        error("Expected Right, not Left($it)")
    }, {
        verifications(it)
    })
}

private fun <A, B> Either<A, B>.verifyLeft(verifications: (A) -> Unit) {
    fold({
        verifications(it)
    }, {
        error("Expected Left, not Right($it)")
    })
}

@WireMockTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DiscordClientTest {
    private val vertx = Vertx.vertx()
    private val vauhtijuoksuServerId = "123"
    private val adminRoleId = "456"
    private val bearer = "that_means_more_bear"
    private val config =
        DiscordClientConfiguration(
            host = "localhost",
            vauhtijuoksuServerId = vauhtijuoksuServerId,
            adminRoleId = adminRoleId,
        )
    private lateinit var client: DiscordClient

    @BeforeAll
    fun setup(wm: WireMockRuntimeInfo) {
        client = DiscordClient(vertx, config.copy(port = wm.httpPort))
    }

    @Test
    fun `Normal user with no nicknames, but with ignored fields`() =
        runTest {
            val username = "user"
            stubFor(
                get("/api/users/@me/guilds/$vauhtijuoksuServerId/member")
                    .withHeader(
                        "Authorization",
                        matching("Bearer $bearer"),
                    ).willReturn(
                        aResponse().withBody(
                            """
                            {
                              "user": {
                                "username": "$username",
                                "ignoredProperty": "ignoredValue"
                              },
                              "roles": ["789"]
                            }
                            """.trimIndent(),
                        ),
                    ),
            )

            client.getUser(bearer).verifyRight {
                it.displayName shouldBe username
                it.isAdmin shouldBe false
            }
        }

    @Test
    fun `Admin users are recognized by the role`() =
        runTest {
            val username = "user"
            stubFor(
                get("/api/users/@me/guilds/$vauhtijuoksuServerId/member")
                    .withHeader(
                        "Authorization",
                        matching("Bearer $bearer"),
                    ).willReturn(
                        aResponse().withBody(
                            """
                            {
                              "user": {
                                "username": "$username"
                              },
                              "roles": ["789", "$adminRoleId"]
                            }
                            """.trimIndent(),
                        ),
                    ),
            )

            client.getUser(bearer).verifyRight {
                it.displayName shouldBe "user"
                it.isAdmin shouldBe true
            }
        }

    @Test
    fun `Guild nickname is used if set`() =
        runTest {
            val guildNick = "guild user"
            stubFor(
                get("/api/users/@me/guilds/$vauhtijuoksuServerId/member")
                    .withHeader(
                        "Authorization",
                        matching("Bearer $bearer"),
                    ).willReturn(
                        aResponse().withBody(
                            """
                            {
                              "user": {
                                "username": "user",
                                "global_name": "global name"
                              },
                              "nick": "$guildNick",
                              "roles": []
                            }
                            """.trimIndent(),
                        ),
                    ),
            )

            client.getUser(bearer).verifyRight {
                it.displayName shouldBe guildNick
                it.isAdmin shouldBe false
            }
        }

    @Test
    fun `Global name is used if set and guild nickname is not`() =
        runTest {
            val globalNickname = "Global nick"
            stubFor(
                get("/api/users/@me/guilds/$vauhtijuoksuServerId/member")
                    .withHeader(
                        "Authorization",
                        matching("Bearer $bearer"),
                    ).willReturn(
                        aResponse().withBody(
                            """
                            {
                              "user": {
                                "username": "user",
                                "global_name": "$globalNickname"
                              },
                              "nick": null,
                              "roles": ["789"]
                            }
                            """.trimIndent(),
                        ),
                    ),
            )

            client.getUser(bearer).verifyRight {
                it.displayName shouldBe globalNickname
                it.isAdmin shouldBe false
            }
        }

    @Test
    fun `When user is not a member, error is returned`() =
        runTest {
            stubFor(
                get("/api/users/@me/guilds/$vauhtijuoksuServerId/member")
                    .withHeader(
                        "Authorization",
                        matching("Bearer $bearer"),
                    ).willReturn(
                        aResponse().withStatus(404).withBody("""{"message": "Unknown Server", "code": 10004}"""),
                    ),
            )

            client.getUser(bearer).verifyLeft {
                it shouldBe DiscordError.NOT_A_MEMBER
            }
        }
}
