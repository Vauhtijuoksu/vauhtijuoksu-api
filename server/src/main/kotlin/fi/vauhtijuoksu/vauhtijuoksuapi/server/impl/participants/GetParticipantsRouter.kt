package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.participants

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Participant
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.GetRouter
import io.vertx.ext.web.handler.CorsHandler
import jakarta.inject.Inject
import jakarta.inject.Named

class GetParticipantsRouter
@Inject constructor(
    @Named(DependencyInjectionConstants.PUBLIC_CORS)
    publicEndpointCorsHandler: CorsHandler,
    participantDatabase: VauhtijuoksuDatabase<Participant>,
) : GetRouter<Participant>(
    publicEndpointCorsHandler,
    participantDatabase,
    { participant: Participant -> ParticipantResponse.fromParticipant(participant).toJson() },
)
