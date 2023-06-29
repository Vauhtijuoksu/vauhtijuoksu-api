package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.players

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Player
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.GetRouter
import io.vertx.ext.web.handler.CorsHandler
import jakarta.inject.Inject
import jakarta.inject.Named

class GetPlayersRouter
@Inject constructor(
    @Named(DependencyInjectionConstants.PUBLIC_CORS)
    publicEndpointCorsHandler: CorsHandler,
    playerDatabase: VauhtijuoksuDatabase<Player>,
) : GetRouter<Player>(
    publicEndpointCorsHandler,
    playerDatabase,
    { player: Player -> PlayerResponse.fromPlayer(player).toJson() },
)
