package fi.vauhtijuoksu.vauhtijuoksuapi.server.configuration

import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration

data class Config(val server: ServerConfiguration, val database: DatabaseConfiguration)
