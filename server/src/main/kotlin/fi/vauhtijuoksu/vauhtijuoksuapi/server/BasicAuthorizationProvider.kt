package fi.vauhtijuoksu.vauhtijuoksuapi.server

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.ext.auth.User
import io.vertx.ext.auth.authorization.AuthorizationProvider
import io.vertx.ext.auth.authorization.RoleBasedAuthorization

/**
 * All basic auth users are admins. There seems to be no way to see which authentication method was used,
 * but OAuth doesn't set the pwd amr claim so that identifies basic auth users.
 */
class BasicAuthorizationProvider : AuthorizationProvider {
    override fun getId(): String = "Basic authorization provider"

    override fun getAuthorizations(
        user: User,
        handler: Handler<AsyncResult<Void>>,
    ) {
        if (user.hasAmr("pwd")) {
            user.authorizations().add(id, RoleBasedAuthorization.create(Roles.ADMIN.name))
        }
        handler.handle(Future.succeededFuture())
    }
}
