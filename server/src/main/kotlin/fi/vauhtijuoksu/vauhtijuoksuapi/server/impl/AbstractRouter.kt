package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.Mapper
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PatchInputValidator
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PostInputValidator
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import mu.KotlinLogging
import java.util.UUID

@Suppress("UnnecessaryAbstractClass") // Abstract is used to indicate the intended usage
abstract class AbstractRouter<T : Model>
@Suppress("LongParameterList") // I think this is fine as the parameters are independent toggles
protected constructor(
    private val endpoint: String,
    private val mapper: Mapper<T>,
    private val db: VauhtijuoksuDatabase<T>,
    private val authenticationHandler: AuthenticationHandler,
    private val authenticatedEndpointCorsHandler: CorsHandler,
    private val publicEndpointCorsHandler: CorsHandler,
    private val allowPost: Boolean,
    private val allowDelete: Boolean,
    private val allowPatch: Boolean,
    private val postInputValidator: PostInputValidator<T>?,
    private val patchInputValidator: PatchInputValidator<T>?,
) {
    companion object {
        const val OK = 200
        const val CREATED = 201
        const val NO_CONTENT = 204
        const val BAD_REQUEST = 400
        const val NOT_FOUND = 404
        const val METHOD_NOT_ALLOWED = 405
        const val INTERNAL_SERVER_ERROR = 500
    }

    private val logger = KotlinLogging.logger {}

    init {
        DatabindCodec.mapper()
            .registerModule(kotlinModule())

        if (!endpoint.startsWith("/")) {
            throw IllegalArgumentException("Endpoint should start with /")
        }
        if (allowPost && postInputValidator == null) {
            throw IllegalArgumentException("postInputValidator is required if allowPost is true")
        }
        if (allowPatch && patchInputValidator == null) {
            throw IllegalArgumentException("patchInputValidator is required if allowPost is true")
        }
    }

    fun configure(router: Router) {
        router.route("$endpoint*").handler { ctx ->
            ctx.response().putHeader("content-type", "application/json")
            ctx.next()
        }
        get(router)

        if (allowPost) {
            post(router)
        }
        if (allowPatch) {
            patch(router)
        }
        if (allowDelete) {
            delete(router)
        }
        router.route(endpoint).handler { ctx ->
            ctx.response().setStatusCode(METHOD_NOT_ALLOWED).end()
        }
        router.route("$endpoint/:id").handler { ctx ->
            ctx.response().setStatusCode(METHOD_NOT_ALLOWED).end()
        }
    }

    private fun get(router: Router) {
        router.get(endpoint)
            .handler(publicEndpointCorsHandler)
            .handler { ctx ->
                db.getAll()
                    .onFailure { t ->
                        logger.warn { "Failed to retrieve record because of ${t.message}" }
                        ctx.response().setStatusCode(INTERNAL_SERVER_ERROR).end()
                    }
                    .onSuccess { all -> ctx.response().end(jacksonObjectMapper().writeValueAsString(all)) }
            }

        router.get("$endpoint/:id")
            .handler(publicEndpointCorsHandler)
            .handler { ctx ->
                val id: UUID
                try {
                    id = UUID.fromString(ctx.pathParam("id"))
                } catch (e: IllegalArgumentException) {
                    ctx.response().setStatusCode(BAD_REQUEST).end(e.message)
                    return@handler
                }
                db.getById(id)
                    .onFailure { t ->
                        logger.warn { "Failed to get record because of ${t.message}" }
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
    }

    private fun post(router: Router) {
        router.post(endpoint)
            .handler(authenticationHandler)
            .handler(authenticatedEndpointCorsHandler)
            .handler(BodyHandler.create())
            .handler { ctx ->
                val record: T
                try {
                    val jsonBody = ctx.bodyAsJson
                    if (jsonBody == null) {
                        ctx.response().setStatusCode(BAD_REQUEST).end("Body is required on POST")
                        return@handler
                    }
                    record = mapper.mapTo(jsonBody)
                    logger.debug { "Inserting a new record object $record" }
                } catch (e: IllegalArgumentException) {
                    logger.warn { "Error parsing record object from ${ctx.bodyAsString} because ${e.message}" }
                    ctx.response().setStatusCode(BAD_REQUEST).end(e.message)
                    return@handler
                }

                // Asserted to be non-null in init
                val validationMessage = postInputValidator!!.validate(record)
                if (validationMessage != null) {
                    ctx.response().setStatusCode(BAD_REQUEST).end(validationMessage)
                    return@handler
                }

                db.add(record)
                    .onFailure { t ->
                        logger.warn { "Failed to insert record $record because of ${t.message}" }
                        ctx.response().setStatusCode(INTERNAL_SERVER_ERROR).end(t.message)
                    }
                    .onSuccess { insertedGd ->
                        logger.info { "Inserted record $insertedGd" }
                        ctx.response().setStatusCode(CREATED)
                            .end(jacksonObjectMapper().writeValueAsString(insertedGd))
                    }
            }
    }

    private fun delete(router: Router) {
        router.delete("$endpoint/:id")
            .handler(authenticationHandler)
            .handler(authenticatedEndpointCorsHandler)
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
                        logger.warn { "Failed to delete record with id $id because of ${t.message}" }
                    }
                    .onSuccess { res ->
                        if (res) {
                            ctx.response().setStatusCode(NO_CONTENT).end()
                        } else {
                            ctx.response().setStatusCode(NOT_FOUND).end()
                        }
                    }
            }
    }

    @Suppress("LongMethod") // This will probably shorten once there is centralized error handling
    private fun patch(router: Router) {
        router.patch("$endpoint/:id")
            .handler(authenticationHandler)
            .handler(authenticatedEndpointCorsHandler)
            .handler(BodyHandler.create())
            .handler { ctx ->
                val id: UUID
                try {
                    id = UUID.fromString(ctx.pathParam("id"))
                } catch (e: IllegalArgumentException) {
                    ctx.response().setStatusCode(BAD_REQUEST).end(e.message)
                    return@handler
                }

                val record: T
                try {
                    val jsonBody = ctx.bodyAsJson
                    if (jsonBody == null) {
                        ctx.response().setStatusCode(BAD_REQUEST).end("Body is required on PATCH")
                        return@handler
                    }
                    record = mapper.mapTo(jsonBody)
                    logger.debug { "Patching a record with object $record" }
                } catch (e: IllegalArgumentException) {
                    logger.warn { "Error parsing record object from ${ctx.bodyAsString} because ${e.message}" }
                    ctx.response().setStatusCode(BAD_REQUEST).end(e.message)
                    return@handler
                }
                db.getById(id)
                    .onFailure { t ->
                        logger.warn { "Failed to patch record $record because of ${t.message}" }
                        ctx.response().setStatusCode(INTERNAL_SERVER_ERROR).end()
                    }.onSuccess { res ->
                        if (res == null) {
                            logger.info { "Could not find record with id $id for update" }
                            ctx.response().setStatusCode(NOT_FOUND).end()
                            return@onSuccess
                        }
                        val oldData = jacksonObjectMapper().readerForUpdating(res)
                        val mergedData: T = oldData.readValue(ctx.bodyAsString)

                        // Asserted to be non-null in init
                        val validationMessage = patchInputValidator!!.validate(mergedData)
                        if (validationMessage != null) {
                            ctx.response().setStatusCode(BAD_REQUEST).end(validationMessage)
                            return@onSuccess
                        }

                        db.update(mergedData)
                            .onFailure { t ->
                                logger.warn { "Failed to insert record $record because of ${t.message}" }
                                ctx.response().setStatusCode(INTERNAL_SERVER_ERROR).end(t.message)
                            }
                            .onSuccess { updatedRecord ->
                                if (updatedRecord == null) {
                                    logger.info { "Could not find record with id $id for update" }
                                    // Donation was removed after fetching it, before patching
                                    ctx.response().setStatusCode(NOT_FOUND).end()
                                    return@onSuccess
                                }
                                logger.info { "Patched record $updatedRecord" }
                                ctx.response().setStatusCode(OK)
                                    .end(jacksonObjectMapper().writeValueAsString(updatedRecord))
                            }
                    }
            }
    }
}
