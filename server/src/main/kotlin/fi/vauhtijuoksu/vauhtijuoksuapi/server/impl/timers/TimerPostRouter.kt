package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.timers

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Timer
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.PostRouter
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.AuthorizationHandler
import io.vertx.ext.web.handler.CorsHandler
import jakarta.inject.Inject
import jakarta.inject.Named
import java.util.UUID

class TimerPostRouter
    @Inject
    constructor(
        authenticationHandler: AuthenticationHandler,
        adminRequired: AuthorizationHandler,
        @Named(DependencyInjectionConstants.AUTHENTICATED_CORS)
        authenticatedEndpointCorsHandler: CorsHandler,
        db: VauhtijuoksuDatabase<Timer>,
    ) : PostRouter<Timer>(
            authenticationHandler,
            adminRequired,
            authenticatedEndpointCorsHandler,
            db,
            { json -> json.mapTo(NewTimerApiModel::class.java).toTimer(UUID.randomUUID()) },
            { timer -> TimerApiModel.from(timer).toJson() },
            { null },
        )
