package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.timers

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.GetRouter
import io.vertx.ext.web.handler.CorsHandler
import javax.inject.Inject
import javax.inject.Named

class TimerGetRouter
@Inject constructor(
    @Named(DependencyInjectionConstants.PUBLIC_CORS)
    publicEndpointCorsHandler: CorsHandler,
    db: VauhtijuoksuDatabase<Timer>,
) : GetRouter<Timer>(
    publicEndpointCorsHandler,
    db,
    { timer -> TimerApiModel.from(timer).toJson() },
)
