package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.google.inject.AbstractModule
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addFileSource
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.Config
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.ServerConfiguration
import java.io.File

class ConfigurationModule : AbstractModule() {
    override fun configure() {
        val config: Config = ConfigLoaderBuilder
            .default()
            // TODO configure this so it's not hard coded
            .addFileSource(File("/configuration/conf.yaml"))
            .addFileSource(File("/configuration/secret-conf.yaml"))
            .build().loadConfigOrThrow()
        bind(ServerConfiguration::class.java).toInstance(config.server)
        bind(DatabaseConfiguration::class.java).toInstance(config.database)
    }
}
