package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.websockettemplate

import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.WebsocketTemplate
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiModel
import io.vertx.core.json.JsonObject
import java.util.*

data class WebSocketTemplateModel(
        val uuid: UUID,
        @JsonProperty("payload")
        val message: String
): ApiModel<WebsocketTemplate> {
    override fun toModel(): WebsocketTemplate {
        TODO("Not yet implemented")
    }

    override fun toJson(): JsonObject {
        TODO("Not yet implemented")
    }

}