package fi.vauhtijuoksu.vauhtijuoksuapi.server.api

import io.vertx.core.json.JsonObject

fun interface Mapper<T> {
    fun mapTo(json: JsonObject): T
}
