package fi.vauhtijuoksu.vauhtijuoksuapi.database.configuration

data class DatabaseConfiguration(
    val address: String,
    val port: Int,
    val database: String,
    val user: String,
    val password: String,
    val poolSize: Int
)
