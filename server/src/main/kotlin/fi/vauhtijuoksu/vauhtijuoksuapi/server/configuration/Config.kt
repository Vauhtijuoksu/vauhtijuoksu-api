package fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration

import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration

data class Config(
    val server: ServerConfiguration,
    val database: DatabaseConfiguration,
    val oAuth: OAuthConfiguration,
    val discordClient: DiscordClientConfiguration,
    val redis: RedisConfiguration?,
)

data class RedisConfiguration(
    val host: String,
    val password: String,
)

data class OAuthConfiguration(
    val clientId: String,
    val clientSecret: String,
    val callbackUrl: String,
    val baseAuthorizationUrl: String = "https://discord.com",
    val authorizationPath: String = "/oauth2/authorize",
    val tokenPath: String = "/api/oauth2/token",
    val tokenRevocationUrl: String = "/api/oauth2/token/revoke",
    val scopes: List<String> = listOf("identify", "guilds.members.read"),
)

data class DiscordClientConfiguration(
    val host: String = "discord.com",
    val port: Int = 80,
    val vauhtijuoksuServerId: String,
    val adminRoleId: String,
)

data class ServerConfiguration(
    val port: Int,
    val htpasswdFileLocation: String,
    val corsHeader: String,
)
