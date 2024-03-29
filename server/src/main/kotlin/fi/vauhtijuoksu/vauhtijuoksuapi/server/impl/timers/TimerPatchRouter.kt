package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.timers

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.PatchRouter
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.AuthorizationHandler
import io.vertx.ext.web.handler.CorsHandler
import jakarta.inject.Inject
import jakarta.inject.Named

class TimerPatchRouter
@Inject constructor(
    authenticationHandler: AuthenticationHandler,
    adminRequired: AuthorizationHandler,
    @Named(DependencyInjectionConstants.AUTHENTICATED_CORS)
    authenticatedEndpointCorsHandler: CorsHandler,
    db: VauhtijuoksuDatabase<Timer>,
) : PatchRouter<Timer, TimerApiModel>(
    authenticationHandler,
    adminRequired,
    authenticatedEndpointCorsHandler,
    db,
    { null },
    { timer -> TimerApiModel.from(timer) },
)
