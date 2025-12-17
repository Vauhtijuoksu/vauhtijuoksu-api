package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.gamedata

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants.Companion.AUTHENTICATED_CORS
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.PostRouter
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.AuthorizationHandler
import io.vertx.ext.web.handler.CorsHandler
import jakarta.inject.Inject
import jakarta.inject.Named
import java.util.UUID

class GameDataPostRouter
    @Inject
    constructor(
        authenticationHandler: AuthenticationHandler,
        adminRequired: AuthorizationHandler,
        @Named(AUTHENTICATED_CORS)
        authenticatedEndpointCorsHandler: CorsHandler,
        db: VauhtijuoksuDatabase<GameData>,
        postInputValidator: GameDataPostInputValidator,
    ) : PostRouter<GameData>(
            authenticationHandler,
            adminRequired,
            authenticatedEndpointCorsHandler,
            db,
            { json -> json.mapTo(NewGameDataApiModel::class.java).toGameData(UUID.randomUUID()) },
            { gameData -> GameDataApiModel.fromGameData(gameData).toJson() },
            postInputValidator,
        )
