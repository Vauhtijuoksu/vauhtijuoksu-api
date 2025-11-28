package fi.vauhtijuoksu.vauhtijuoksuapi.server.api

import io.vertx.ext.web.Router

interface PartialRouter {
    fun configure(
        router: Router,
        basepath: String,
    )
}
