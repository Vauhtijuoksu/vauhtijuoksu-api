package fi.vauhtijuoksu.vauhtijuoksuapi.server.discord

import fi.vauhtijuoksu.vauhtijuoksuapi.server.Roles
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.auth.User
import io.vertx.ext.auth.authorization.AuthorizationProvider
import io.vertx.ext.auth.authorization.RoleBasedAuthorization
import io.vertx.kotlin.coroutines.dispatcher
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import mu.KotlinLogging

class DiscordUserHandler
    @Inject
    constructor(
        private val vertx: Vertx,
        private val discordClient: DiscordClient,
    ) : AuthorizationProvider {
        private val logger = KotlinLogging.logger {}

        override fun getId() = "Discord user role based authorization"

        override fun getAuthorizations(
            user: User,
            handler: Handler<AsyncResult<Void>>,
        ) {
            getAuthorizations(user).onComplete(handler)
        }

        override fun getAuthorizations(user: User): Future<Void> {
            val accessToken = user.principal().getString("access_token")
            return if (accessToken == null) {
                Future.succeededFuture()
            } else {
                Future.future {
                    CoroutineScope(vertx.dispatcher())
                        .async {
                            discordClient.getUser(accessToken).fold({
                                logger.info { "User not a member of Vauhtijuoksu server" }
                            }, {
                                if (it.isAdmin) {
                                    user.authorizations().add(id, RoleBasedAuthorization.create(Roles.ADMIN.name))
                                }
                            })
                        }.invokeOnCompletion { t ->
                            t?.run { it.fail(t) } ?: it.complete()
                        }
                }
            }
        }
    }
