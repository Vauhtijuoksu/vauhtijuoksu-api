package fi.vauhtijuoksu.vauhtijuoksuapi.server

import com.google.inject.AbstractModule
import com.sksamuel.hoplite.ConfigLoader
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.Config
import fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration.ServerConfiguration

class ConfigurationModule : AbstractModule() {
    override fun configure() {
        val config: Config = ConfigLoader().loadConfigOrThrow("/configuration/conf.yaml")
        bind(ServerConfiguration::class.java).toInstance(config.server)
    }
}
