package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.gamedata

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants.Companion.PUBLIC_CORS
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.GetRouter
import io.vertx.ext.web.handler.CorsHandler
import jakarta.inject.Inject
import jakarta.inject.Named

class GameDataGetRouter
@Inject constructor(
    @Named(PUBLIC_CORS)
    publicEndpointCorsHandler: CorsHandler,
    db: VauhtijuoksuDatabase<GameData>,
) : GetRouter<GameData>(
    publicEndpointCorsHandler,
    db,
    { gd: GameData -> GameDataApiModel.fromGameData(gd).toJson() },
)
