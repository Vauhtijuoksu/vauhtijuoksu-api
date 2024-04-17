package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.players

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Participant
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.PostRouter
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.AuthorizationHandler
import io.vertx.ext.web.handler.CorsHandler
import jakarta.inject.Inject
import jakarta.inject.Named
import java.util.UUID

class AddPlayerRouter
@Inject constructor(
    authenticationHandler: AuthenticationHandler,
    adminRequired: AuthorizationHandler,
    @Named(DependencyInjectionConstants.AUTHENTICATED_CORS)
    private val authenticatedEndpointCorsHandler: CorsHandler,
    db: VauhtijuoksuDatabase<Participant>,
) : PostRouter<Participant>(
    authenticationHandler,
    adminRequired,
    authenticatedEndpointCorsHandler,
    db,
    { json -> json.mapTo(NewPlayerRequest::class.java).toParticipant(UUID.randomUUID()) },
    { player -> PlayerResponse.fromParticipant(player).toJson() },
    { null },
)
