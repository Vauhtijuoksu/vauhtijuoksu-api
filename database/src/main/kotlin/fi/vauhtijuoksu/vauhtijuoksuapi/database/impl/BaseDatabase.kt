package fi.vauhtijuoksu.vauhtijuoksuapi.database.impl

import com.fasterxml.jackson.module.kotlin.kotlinModule
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import io.vertx.core.json.jackson.DatabindCodec
import org.flywaydb.core.Flyway

/**
 * Initializes a new database from migrations and setups jackson databind for kotlin
 */
open class BaseDatabase(
    configuration: DatabaseConfiguration,
) {
    init {
        val migrations =
            Flyway
                .configure()
                .dataSource(
                    "jdbc:postgresql://${configuration.address}:${configuration.port}/${configuration.database}?sslmode=prefer",
                    configuration.user,
                    configuration.password,
                ).load()
        migrations.migrate()

        DatabindCodec.mapper().registerModule(kotlinModule())
        DatabindCodec.prettyMapper().registerModule(kotlinModule())
    }
}
