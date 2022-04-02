package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.Mapper
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PatchInputValidator
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PostInputValidator
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.CorsHandler
import javax.inject.Inject
import javax.inject.Named

class GameDataRouter @Inject constructor(
    db: VauhtijuoksuDatabase<GameData>,
    authenticationHandler: AuthenticationHandler,
    @Named(DependencyInjectionConstants.AUTHENTICATED_CORS)
    authenticatedEndpointCorsHandler: CorsHandler,
    @Named(DependencyInjectionConstants.PUBLIC_CORS)
    publicEndpointCorsHandler: CorsHandler,
    postInputValidator: PostInputValidator<GameData>?,
    patchInputValidator: PatchInputValidator<GameData>?,
) :
    AbstractRouter<GameData>(
        "/gamedata",
        Mapper { json -> json.mapTo(GameData::class.java) },
        db,
        authenticationHandler,
        authenticatedEndpointCorsHandler,
        publicEndpointCorsHandler,
        allowPost = true,
        allowDelete = true,
        allowPatch = true,
        postInputValidator,
        patchInputValidator
    )
