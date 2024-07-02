package fi.vauhtijuoksu.vauhtijuoksuapi.database

import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.TypeLiteral
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.GeneratedIncentiveCodeDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.ObservableDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.ObservableVauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.SingletonDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration.DatabaseConfiguration
import fi.vauhtijuoksu.vauhtijuoksuapi.database.impl.DonationDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.impl.GameDataDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.impl.GeneratedIncentiveCodeDatabaseImpl
import fi.vauhtijuoksu.vauhtijuoksuapi.database.impl.IncentiveDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.impl.InternalDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.impl.ObservableDatabaseImpl
import fi.vauhtijuoksu.vauhtijuoksuapi.database.impl.ParticipantDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.impl.PlayerInfoDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.impl.TimerDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Participant
import fi.vauhtijuoksu.vauhtijuoksuapi.models.PlayerInfo
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.pgclient.SslMode
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient

class DatabaseModule : AbstractModule() {
    override fun configure() {
        bind(object : TypeLiteral<VauhtijuoksuDatabase<GameData>>() {}).to(object : TypeLiteral<ObservableVauhtijuoksuDatabase<GameData>>() {})

        bind(object : TypeLiteral<ObservableDatabase<GameData>>() {}).to(object :
            TypeLiteral<ObservableVauhtijuoksuDatabase<GameData>>() {})

        bind(object : TypeLiteral<ObservableVauhtijuoksuDatabase<GameData>>() {}).to(object :
            TypeLiteral<ObservableDatabaseImpl<GameData>>() {})

        bind(object : TypeLiteral<InternalDatabase<GameData>>() {}).to(GameDataDatabase::class.java)

        bind(object : TypeLiteral<VauhtijuoksuDatabase<Donation>>() {}).to(DonationDatabase::class.java)
        bind(object : TypeLiteral<VauhtijuoksuDatabase<Incentive>>() {}).to(IncentiveDatabase::class.java)
        bind(object : TypeLiteral<VauhtijuoksuDatabase<Participant>>() {}).to(ParticipantDatabase::class.java)
        bind(object : TypeLiteral<SingletonDatabase<PlayerInfo>>() {}).to(PlayerInfoDatabase::class.java)
        bind(object : TypeLiteral<VauhtijuoksuDatabase<Timer>>() {}).to(TimerDatabase::class.java)
        bind(GeneratedIncentiveCodeDatabase::class.java).to(GeneratedIncentiveCodeDatabaseImpl::class.java)
        bind(SqlClient::class.java).to(PgPool::class.java)
    }

    @Provides
    @Singleton
    fun getSqlClient(config: DatabaseConfiguration): PgPool {
        return PgPool.pool(
            PgConnectOptions().setHost(config.address).setPort(config.port).setSslMode(SslMode.PREFER)
                .setDatabase(config.database).setUser(config.user).setPassword(config.password),
            PoolOptions().setMaxSize(config.poolSize),
        )
    }
}
