package fi.vauhtijuoksu.vauhtijuoksuapi.server.api

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model
import io.vertx.core.json.JsonObject

interface ApiModel<M : Model> {
    fun toModel(): M

    fun toJson(): JsonObject
}
