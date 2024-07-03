package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.gamedata

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.ObservableVauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants.Companion.PUBLIC_CORS
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.WebsocketRouterForModels
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.GetRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.ModelWebSocketRouter
import io.vertx.ext.web.handler.CorsHandler
import jakarta.inject.Inject
import jakarta.inject.Named
import kotlinx.coroutines.CoroutineDispatcher

class GameDataGetRouter
@Inject constructor(
    @Named(PUBLIC_CORS)
    publicEndpointCorsHandler: CorsHandler,
    db: ObservableVauhtijuoksuDatabase<GameData>,
    coroutineDispatcher: CoroutineDispatcher,
) : GetRouter<GameData>(
    publicEndpointCorsHandler,
    db,
    { gd: GameData -> GameDataApiModel.fromGameData(gd).toJson() },
),
    WebsocketRouterForModels<GameData> by ModelWebSocketRouter(
        coroutineDispatcher,
        db,
        { gd: GameData -> jacksonObjectMapper().writeValueAsString(GameDataApiModel.fromGameData(gd)) },
    )
