package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.gamedata

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.DeleteRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.CorsHandler
import javax.inject.Inject
import javax.inject.Named

class GameDataDeleteRouter @Inject constructor(
    private val authenticationHandler: AuthenticationHandler,
    @Named(DependencyInjectionConstants.AUTHENTICATED_CORS)
    private val authenticatedEndpointCorsHandler: CorsHandler,
    private val db: VauhtijuoksuDatabase<GameData>,
) :
    DeleteRouter<GameData>(
        authenticationHandler,
        authenticatedEndpointCorsHandler,
        db,
    )
