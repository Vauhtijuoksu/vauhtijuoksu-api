package fi.vauhtijuoksu.vauhtijuoksuapi.server.api

import io.vertx.ext.web.Router

interface ApiRouter {
    fun router(): Router
}
