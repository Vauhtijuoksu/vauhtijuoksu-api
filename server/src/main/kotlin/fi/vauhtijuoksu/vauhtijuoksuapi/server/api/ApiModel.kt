package fi.vauhtijuoksu.vauhtijuoksuapi.server.api

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model
import io.vertx.core.json.JsonObject

/**
 * ApiModel is something, which can be mapped to a real model and JsonObject
 */
interface ApiModel<M : Model> {
    fun toModel(): M

    fun toJson(): JsonObject
}
