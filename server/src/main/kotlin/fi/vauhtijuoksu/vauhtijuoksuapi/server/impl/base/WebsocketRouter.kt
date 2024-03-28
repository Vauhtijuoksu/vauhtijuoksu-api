package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PartialRouter
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router

open class WebsocketRouter<M: Model>(
        private val toJson: ((M) -> JsonObject),
        private val db: VauhtijuoksuDatabase<M>,
): PartialRouter {
    override fun configure(router: Router, basepath: String) {
        configureWebSocket(router, basepath)
    }

    private fun configureWebSocket(router: Router, basepath: String) {
        router.route("ws://$basepath").handler{ctx->
            val request = ctx.request()
            val fut = request.toWebSocket()
            fut.onSuccess{ws->

            }
            fut.onFailure(ctx::fail)
        }
    }
}