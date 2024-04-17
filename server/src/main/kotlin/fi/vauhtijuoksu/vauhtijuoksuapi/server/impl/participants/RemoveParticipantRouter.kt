package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.participants

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Participant
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.DeleteRouter
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.AuthorizationHandler
import io.vertx.ext.web.handler.CorsHandler
import jakarta.inject.Inject
import jakarta.inject.Named

class RemoveParticipantRouter
@Inject constructor(
    authenticationHandler: AuthenticationHandler,
    adminRequired: AuthorizationHandler,
    @Named(DependencyInjectionConstants.AUTHENTICATED_CORS) private val authenticatedEndpointCorsHandler: CorsHandler,
    db: VauhtijuoksuDatabase<Participant>,
) : DeleteRouter<Participant>(
    authenticationHandler,
    adminRequired,
    authenticatedEndpointCorsHandler,
    db,
)
