package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.SingletonDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.UserError
import fi.vauhtijuoksu.vauhtijuoksuapi.models.PlayerInfo
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import io.vertx.core.Future.future
import io.vertx.core.Promise
import io.vertx.core.json.DecodeException
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.AuthorizationHandler
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import jakarta.inject.Inject
import jakarta.inject.Named

// Throwing as soon as some validation fails is the simplest way
@Suppress("ThrowsCount")
class PlayerInfoRouter
    @Inject
    constructor(
        private val db: SingletonDatabase<PlayerInfo>,
        private val authenticationHandler: AuthenticationHandler,
        private val adminRequired: AuthorizationHandler,
        @Named(DependencyInjectionConstants.AUTHENTICATED_CORS)
        private val authenticatedEndpointCorsHandler: CorsHandler,
        @Named(DependencyInjectionConstants.PUBLIC_CORS)
        private val publicEndpointCorsHandler: CorsHandler,
    ) {
        private val playerInfo =
            JsonObject(
                """{
            "message": null
        }""",
            )

        @Suppress("SwallowedException") // Not swallowed, the message is used in augmented error
        fun configure(router: Router) {
            router.route("/player-info").handler { ctx ->
                ctx.response().putHeader("content-type", "application/json")
                ctx.next()
            }
            router
                .get("/player-info")
                .handler(publicEndpointCorsHandler)
                .handler { ctx ->
                    db
                        .get()
                        .onSuccess {
                            ctx
                                .response()
                                .end(jacksonObjectMapper().writeValueAsString(it))
                        }.onFailure(ctx::fail)
                }
            router
                .patch("/player-info")
                .handler(authenticatedEndpointCorsHandler)
                .handler(BodyHandler.create())
                .handler(authenticationHandler)
                .handler(adminRequired)
                .handler { ctx ->
                    db
                        .get()
                        .flatMap { existingPlayerInfo ->
                            val body =
                                try {
                                    ctx.body().asJsonObject()
                                } catch (e: DecodeException) {
                                    throw UserError("Invalid json ${e.message}")
                                }
                            body.fieldNames().forEach { key ->
                                if (!playerInfo.containsKey(key)) {
                                    throw UserError("Unknown key in request: $key")
                                }
                            }
                            val newData =
                                try {
                                    updateFromJson(existingPlayerInfo, body)
                                } catch (e: InvalidFormatException) {
                                    throw UserError("Invalid request: ${e.message}")
                                }
                            return@flatMap future { p: Promise<PlayerInfo> ->
                                db
                                    .save(newData)
                                    .onSuccess {
                                        p.complete(newData)
                                    }.onFailure(p::fail)
                            }
                        }.onSuccess {
                            ctx
                                .response()
                                .end(jacksonObjectMapper().writeValueAsString(it))
                        }.onFailure(ctx::fail)
                }
        }

        private fun updateFromJson(
            data: PlayerInfo,
            json: JsonObject,
        ): PlayerInfo {
            val oldData = jacksonObjectMapper().readerForUpdating(data)
            val mergedData: PlayerInfo = oldData.readValue(json.encode())
            return PlayerInfo(
                mergedData.message,
            )
        }
    }
