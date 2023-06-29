package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.players

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Player
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.DeleteRouter
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.CorsHandler
import jakarta.inject.Inject
import jakarta.inject.Named

class RemovePLayerRouter
@Inject constructor(
    authenticationHandler: AuthenticationHandler,
    @Named(DependencyInjectionConstants.AUTHENTICATED_CORS) private val authenticatedEndpointCorsHandler: CorsHandler,
    db: VauhtijuoksuDatabase<Player>,
) : DeleteRouter<Player>(
    authenticationHandler,
    authenticatedEndpointCorsHandler,
    db,
)
