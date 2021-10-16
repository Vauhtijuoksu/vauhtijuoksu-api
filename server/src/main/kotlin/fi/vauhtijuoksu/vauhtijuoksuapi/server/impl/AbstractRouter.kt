package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

abstract class AbstractRouter {
    companion object {
        const val CREATED = 201
        const val NO_CONTENT = 204
        const val BAD_REQUEST = 400
        const val NOT_FOUND = 404
        const val INTERNAL_SERVER_ERROR = 500
        const val NOT_IMPLEMENTED = 501
    }

    internal fun notImplemented(ctx: RoutingContext) {
        ctx.response().setStatusCode(NOT_IMPLEMENTED).end()
    }

    abstract fun configure(router: Router)
}
