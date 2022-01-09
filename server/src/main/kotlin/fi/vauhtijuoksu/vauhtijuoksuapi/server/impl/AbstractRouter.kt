package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.MissingEntityException
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.UserError
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.VauhtijuoksuException
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Model
import fi.vauhtijuoksu.vauhtijuoksuapi.server.ApiConstants.Companion.CREATED
import fi.vauhtijuoksu.vauhtijuoksuapi.server.ApiConstants.Companion.METHOD_NOT_ALLOWED
import fi.vauhtijuoksu.vauhtijuoksuapi.server.ApiConstants.Companion.NO_CONTENT
import fi.vauhtijuoksu.vauhtijuoksuapi.server.ApiConstants.Companion.OK
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

// Throwing as soon as some validation fails is the simplest way
@Suppress("ThrowsCount")
open class AbstractRouter<T : Model>
// I think this is fine as the parameters are independent toggles
@Suppress("LongParameterList")
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
                        ctx.fail(ServerError("Failed to retrieve record because of ${t.message}"))
                    }
                    .onSuccess { all -> ctx.response().end(jacksonObjectMapper().writeValueAsString(all)) }
            }

        router.get("$endpoint/:id")
            .handler(publicEndpointCorsHandler)
            .handler { ctx ->
                val id: UUID
                try {
                    id = UUID.fromString(ctx.pathParam("id"))
                } catch (_: IllegalArgumentException) {
                    throw UserError("Not UUID: ${ctx.pathParam("id")}")
                }
                db.getById(id)
                    .onFailure(ctx::fail)
                    .onSuccess { gameData ->
                        if (gameData == null) {
                            ctx.fail(MissingEntityException("No entity with id $id"))
                        } else {
                            ctx.response().end(jacksonObjectMapper().writeValueAsString(gameData))
                        }
                    }
            }
    }

    private fun post(router: Router) {
        router.post(endpoint)
            .handler(authenticatedEndpointCorsHandler)
            .handler(authenticationHandler)
            .handler(BodyHandler.create())
            .handler { ctx ->
                val record: T
                try {
                    val jsonBody = ctx.bodyAsJson
                    if (jsonBody == null) {
                        ctx.fail(UserError("Body is required on POST"))
                        return@handler
                    }
                    record = mapper.mapTo(jsonBody)
                    logger.debug { "Inserting a new record object $record" }
                } catch (e: IllegalArgumentException) {
                    ctx.fail(UserError("Error parsing record object from ${ctx.bodyAsString} because ${e.message}"))
                    return@handler
                }

                // Asserted to be non-null in init
                val validationMessage = postInputValidator!!.validate(record)
                if (validationMessage != null) {
                    ctx.fail(UserError(validationMessage))
                    return@handler
                }

                db.add(record)
                    .onFailure(ctx::fail)
                    .onSuccess { insertedGd ->
                        logger.info { "Inserted record $insertedGd" }
                        ctx.response().setStatusCode(CREATED)
                            .end(jacksonObjectMapper().writeValueAsString(insertedGd))
                    }
            }
    }

    private fun delete(router: Router) {
        router.delete("$endpoint/:id")
            .handler(authenticatedEndpointCorsHandler)
            .handler(authenticationHandler)
            .handler { ctx ->
                val id: UUID
                try {
                    id = UUID.fromString(ctx.pathParam("id"))
                } catch (_: IllegalArgumentException) {
                    throw UserError("Not UUID: ${ctx.pathParam("id")}")
                }
                db.delete(id)
                    .onFailure(ctx::fail)
                    .onSuccess { res ->
                        if (res) {
                            ctx.response().setStatusCode(NO_CONTENT).end()
                        } else {
                            ctx.fail(MissingEntityException("No entity with id $id"))
                        }
                    }
            }
    }

    private fun patch(router: Router) {
        router.patch("$endpoint/:id")
            .handler(authenticatedEndpointCorsHandler)
            .handler(authenticationHandler)
            .handler(BodyHandler.create())
            .handler { ctx ->
                val id: UUID
                try {
                    id = UUID.fromString(ctx.pathParam("id"))
                } catch (_: IllegalArgumentException) {
                    throw UserError("Not UUID: ${ctx.pathParam("id")}")
                }

                @Suppress("SwallowedException") // Not swallowed, the message is used in augmented error
                val record: T = try {
                    val jsonBody = ctx.bodyAsJson ?: throw VauhtijuoksuException("Body is required on PATCH")
                    mapper.mapTo(jsonBody)
                } catch (e: IllegalArgumentException) {
                    throw UserError("Error parsing record object from ${ctx.bodyAsString} because ${e.message}")
                }
                logger.debug { "Patching a record with object $record" }
                db.getById(id)
                    .map { res ->
                        if (res == null) {
                            throw MissingEntityException("Could not find record with id $id for update")
                        }
                        val oldData = jacksonObjectMapper().readerForUpdating(res)
                        val mergedData: T = oldData.readValue(ctx.bodyAsString)

                        // Asserted to be non-null in init
                        val validationMessage = patchInputValidator!!.validate(mergedData)
                        if (validationMessage != null) {
                            throw UserError("Invalid input: $validationMessage")
                        }
                        return@map mergedData
                    }.compose { db.update(it) }
                    .map {
                        // Donation was removed after fetching it, before patching
                        return@map it ?: MissingEntityException("Could not find record with id $id for update")
                    }.onSuccess { updatedRecord ->
                        logger.info { "Patched record $updatedRecord" }
                        ctx.response().setStatusCode(OK)
                            .end(jacksonObjectMapper().writeValueAsString(updatedRecord))
                    }
                    .onFailure(ctx::fail)
            }
    }
}
