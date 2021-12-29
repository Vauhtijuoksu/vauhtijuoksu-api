package fi.vauhtijuoksu.vauhtijuoksuapi.database

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.TypeLiteral
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.SingletonDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.database.impl.DonationDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.impl.GameDataDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.impl.StreamMetadataDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.models.StreamMetadata
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.SslMode
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient

class DatabaseModule : AbstractModule() {
    override fun configure() {
        bind(object : TypeLiteral<VauhtijuoksuDatabase<GameData>>() {}).to(GameDataDatabase::class.java)
        bind(object : TypeLiteral<VauhtijuoksuDatabase<Donation>>() {}).to(DonationDatabase::class.java)
        bind(object : TypeLiteral<SingletonDatabase<StreamMetadata>>() {}).to(StreamMetadataDatabase::class.java)
    }

    @Provides
    @Singleton
    fun getSqlClient(config: DatabaseConfiguration): SqlClient {
        return PgPool.client(
            PgConnectOptions()
                .setHost(config.address)
                .setPort(config.port)
                .setSslMode(SslMode.PREFER)
                .setDatabase(config.database)
                .setUser(config.user)
                .setPassword(config.password),
            PoolOptions().setMaxSize(config.poolSize)
        )
    }
}
