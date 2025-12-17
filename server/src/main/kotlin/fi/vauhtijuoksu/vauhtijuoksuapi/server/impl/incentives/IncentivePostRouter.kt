package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.PostRouter
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.AuthorizationHandler
import io.vertx.ext.web.handler.CorsHandler
import jakarta.inject.Inject
import jakarta.inject.Named
import java.util.UUID

class IncentivePostRouter
    @Inject
    constructor(
        authenticationHandler: AuthenticationHandler,
        adminRequired: AuthorizationHandler,
        @Named(DependencyInjectionConstants.AUTHENTICATED_CORS)
        private val authenticatedEndpointCorsHandler: CorsHandler,
        db: VauhtijuoksuDatabase<Incentive>,
    ) : PostRouter<Incentive>(
            authenticationHandler,
            adminRequired,
            authenticatedEndpointCorsHandler,
            db,
            { json -> json.mapTo(NewIncentiveApiModel::class.java).toIncentive(UUID.randomUUID()) },
            { incentive -> IncentiveApiModel.fromIncentive(incentive).toJson() },
            { null }, // Incentive is always valid
        )
