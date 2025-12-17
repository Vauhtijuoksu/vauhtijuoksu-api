package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.participants

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

class AddParticipantRouter
    @Inject
    constructor(
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
            { json -> json.mapTo(NewParticipantRequest::class.java).toParticipant(UUID.randomUUID()) },
            { player -> ParticipantResponse.fromParticipant(player).toJson() },
            { null },
        )
