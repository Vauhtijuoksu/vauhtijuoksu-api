package fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration

import com.google.inject.AbstractModule
import com.google.inject.util.Providers
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addEnvironmentSource
import com.sksamuel.hoplite.addFileSource
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import java.io.File

class ConfigurationModule : AbstractModule() {
    override fun configure() {
        val config = ConfigLoaderBuilder
            .default()
            .addEnvironmentSource()
            .apply {
                File("/configuration").listFiles()?.filter { it.isFile }?.forEach { addFileSource(it) }
            }
            .build().loadConfigOrThrow<Config>()
        bind(ServerConfiguration::class.java).toInstance(config.server)
        bind(DatabaseConfiguration::class.java).toInstance(config.database)
        bind(OAuthConfiguration::class.java).toInstance(config.oAuth)
        bind(DiscordClientConfiguration::class.java).toInstance(config.discordClient)
        bind(RedisConfiguration::class.java).toProvider(Providers.of(config.redis)) // Might be null, need a workaround
    }
}
