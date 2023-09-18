package fi.vauhtijuoksu.vauhtijuoksuapi.server.discord

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import fi.vauhtijuoksu.vauhtijuoksuapi.server.ApiConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.DiscordClientConfiguration
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.kotlin.coroutines.await
import jakarta.inject.Inject
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class DiscordError {
    NOT_A_MEMBER,
}

data class DiscordUser(
    val displayName: String,
    val isAdmin: Boolean = false,
)

class DiscordClient @Inject constructor(
    vertx: Vertx,
    private val discordConfiguration: DiscordClientConfiguration,
) {
    companion object {
        private const val HTTPS_PORT = 443
    }

    private val webClient: WebClient = WebClient.create(
        vertx,
        WebClientOptions().apply {
            defaultHost = discordConfiguration.host
            defaultPort = discordConfiguration.port
        }.setSsl(discordConfiguration.port == HTTPS_PORT),
    )

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun getUser(bearer: String): Either<DiscordError, DiscordUser> {
        val response = webClient.get("/api/users/@me/guilds/${discordConfiguration.vauhtijuoksuServerId}/member")
            .putHeader("Authorization", "Bearer $bearer").send().await()

        return when (response.statusCode()) {
            ApiConstants.OK -> response.bodyAsString().right()
            ApiConstants.NOT_FOUND -> DiscordError.NOT_A_MEMBER.left()
            else -> throw ServerError(
                "Unknown response code from Discord: ${response.statusCode()}\n" +
                    "Response body: ${response.bodyAsString()}",
            )
        }.map {
            json.decodeFromString<GuildMemberShip>(it)
        }.map {
            DiscordUser(
                it.nick ?: it.user.global_name ?: it.user.username,
                it.roles.contains(discordConfiguration.adminRoleId),
            )
        }
    }
}

@Serializable
private data class GuildMemberShip(
    val nick: String?,
    val roles: Set<String>,
    val user: User,
)

@Serializable
private data class User(
    val username: String,
    @Suppress("ConstructorParameterNaming")
    val global_name: String?,
)
