package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.timers

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.PatchRouter
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.CorsHandler
import javax.inject.Inject
import javax.inject.Named

class TimerPatchRouter
@Inject constructor(
    authenticationHandler: AuthenticationHandler,
    @Named(DependencyInjectionConstants.AUTHENTICATED_CORS)
    authenticatedEndpointCorsHandler: CorsHandler,
    db: VauhtijuoksuDatabase<Timer>,
) : PatchRouter<Timer, TimerApiModel>(
    authenticationHandler,
    authenticatedEndpointCorsHandler,
    db,
    { null },
    { timer -> TimerApiModel.from(timer) },
)
