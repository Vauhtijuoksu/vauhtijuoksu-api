package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.BodyHandler
import mu.KotlinLogging
import java.util.UUID
import javax.inject.Inject

class GameDataRouter @Inject constructor(
    private val db: VauhtijuoksuDatabase<GameData>,
    private val inputValidator: GameDataInputValidator,
    private val authenticationHandler: AuthenticationHandler
) :
    AbstractRouter() {
    private val logger = KotlinLogging.logger {}

    init {
        DatabindCodec.mapper()
            .registerModule(kotlinModule())
    }

    @Suppress("LongMethod") // Maybe handlers should be separated, but the whole approach might be modified when
    // generalising the code for other endpoints
    override fun configure(router: Router) {
        router.route("/gamedata*").handler { ctx ->
            ctx.response().putHeader("content-type", "application/json")
            ctx.next()
        }
        router.get("/gamedata").handler { ctx ->
            db.getAll()
                .onFailure { t ->
                    logger.warn { "Failed to retrieve gamedata because of ${t.message}" }
                    ctx.response().setStatusCode(INTERNAL_SERVER_ERROR).end()
                }
                .onSuccess { all -> ctx.response().end(jacksonObjectMapper().writeValueAsString(all)) }
        }
        router.post("/gamedata")
            .handler(authenticationHandler)
            .handler(BodyHandler.create())
            .handler { ctx ->
                val gd: GameData
                try {
                    val jsonBody = ctx.bodyAsJson
                    if (jsonBody == null) {
                        ctx.response().setStatusCode(BAD_REQUEST).end("Body is required on POST")
                        return@handler
                    }
                    gd = ctx.bodyAsJson.mapTo(GameData::class.java)
                    logger.debug { "Inserting a new gamedata object $gd" }
                } catch (e: IllegalArgumentException) {
                    logger.warn { "Error parsing gamedata object from ${ctx.bodyAsJson} because ${e.message}" }
                    ctx.response().setStatusCode(BAD_REQUEST).end(e.message)
                    return@handler
                }

                val validationMessage = inputValidator.validateInput(gd)
                if (validationMessage != null) {
                    ctx.response().setStatusCode(BAD_REQUEST).end(validationMessage)
                    return@handler
                }

                db.add(gd)
                    .onFailure { t ->
                        logger.warn { "Failed to insert gamedata $gd because of ${t.message}" }
                        ctx.response().setStatusCode(INTERNAL_SERVER_ERROR).end(t.message)
                    }
                    .onSuccess { insertedGd ->
                        logger.info { "Inserted gamedata $insertedGd" }
                        ctx.response().setStatusCode(CREATED).end(jacksonObjectMapper().writeValueAsString(insertedGd))
                    }
            }

        router.get("/gamedata/:id").handler { ctx ->
            val id: UUID
            try {
                id = UUID.fromString(ctx.pathParam("id"))
            } catch (e: IllegalArgumentException) {
                ctx.response().setStatusCode(BAD_REQUEST).end(e.message)
                return@handler
            }
            db.getById(id)
                .onFailure { t ->
                    logger.warn { "Failed to get gamedata because of ${t.message}" }
                    ctx.response().setStatusCode(INTERNAL_SERVER_ERROR).end()
                }
                .onSuccess { gameData ->
                    if (gameData == null) {
                        ctx.response().setStatusCode(NOT_FOUND).end()
                    } else {
                        ctx.response().end(jacksonObjectMapper().writeValueAsString(gameData))
                    }
                }
        }

        router.delete("/gamedata/:id")
            .handler(authenticationHandler)
            .handler { ctx ->
                val id: UUID
                try {
                    id = UUID.fromString(ctx.pathParam("id"))
                } catch (e: IllegalArgumentException) {
                    ctx.response().setStatusCode(INTERNAL_SERVER_ERROR).end(e.message)
                    return@handler
                }
                db.delete(id)
                    .onFailure { t ->
                        logger.warn { "Failed to delete gamedata with id $id because of ${t.message}" }
                    }
                    .onSuccess { res ->
                        if (res) {
                            ctx.response().setStatusCode(NO_CONTENT).end()
                        } else {
                            ctx.response().setStatusCode(NOT_FOUND).end()
                        }
                    }
            }

        router.patch("/gamedata/:id")
            .handler(authenticationHandler)
            .handler(this::notImplemented)
    }
}
