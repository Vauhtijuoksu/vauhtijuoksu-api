package fi.vauhtijuoksu.vauhtijuoksuapi.database

import com.google.inject.AbstractModule
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.database.impl.VauhtijuoksuDatabaseImpl

class DatabaseModule : AbstractModule() {
    override fun configure() {
        bind(VauhtijuoksuDatabase::class.java).to(VauhtijuoksuDatabaseImpl::class.java)
    }
}
