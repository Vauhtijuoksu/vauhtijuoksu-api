package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.players

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Player
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiModel
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.PatchRouter
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.CorsHandler
import jakarta.inject.Inject
import jakarta.inject.Named

class ModifyPlayerRouter
@Inject constructor(
    authenticationHandler: AuthenticationHandler,
    @Named(DependencyInjectionConstants.AUTHENTICATED_CORS)
    private val authenticatedEndpointCorsHandler: CorsHandler,
    db: VauhtijuoksuDatabase<Player>,
) : PatchRouter<Player, ApiModel<Player>>(
    authenticationHandler,
    authenticatedEndpointCorsHandler,
    db,
    { null },
    PlayerResponse::fromPlayer,
)
