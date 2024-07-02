package fi.vauhtijuoksu.vauhtijuoksuapi.server.api

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model
import io.vertx.core.Handler
import io.vertx.core.http.ServerWebSocket
import io.vertx.ext.web.Router
import java.util.UUID

interface PartialRouter {
    fun configure(router: Router, basepath: String)
}

interface SubRouter {
    fun configure(router: Router)
}

interface WebSocketRouter<T> {
    suspend fun handler(): Handler<ServerWebSocket>
}

interface WebsocketRouterForModels<T : Model>: WebSocketRouter<T> {
    suspend fun handlerForId(id: UUID): Handler<ServerWebSocket>
}
