package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiRouter
import io.vertx.ext.web.Router
import java.time.Instant
import java.util.Date
import javax.inject.Inject

class ApiRouterImpl @Inject constructor(private val router: Router) : ApiRouter {
    init {
        router.get("/gamedata").handler { ctx ->
            @Suppress("MaxLineLength") // Is a long line yes
            val gameData = GameData("Tetris", "jsloth", Date.from(Instant.now()), Date.from(Instant.now()), "any%", "PC", "1970", null, "tetris.png", "jiisloth")
            ctx.response().end(jacksonObjectMapper().writeValueAsString(gameData))
        }
        router.get().handler { ctx -> ctx.response().end("Hello world") }
    }

    override fun router(): Router {
        return router
    }
}
