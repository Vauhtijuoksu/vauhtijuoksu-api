package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.PostRouter
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.CorsHandler
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

class IncentivePostRouter
@Inject constructor(
    authenticationHandler: AuthenticationHandler,
    @Named(DependencyInjectionConstants.AUTHENTICATED_CORS)
    private val authenticatedEndpointCorsHandler: CorsHandler,
    db: VauhtijuoksuDatabase<Incentive>,
) : PostRouter<Incentive>(
    authenticationHandler,
    authenticatedEndpointCorsHandler,
    db,
    { json -> json.mapTo(NewIncentiveApiModel::class.java).toIncentive(UUID.randomUUID()) },
    { incentive -> IncentiveApiModel.fromIncentive(incentive).toJson() },
    { null }, // Incentive is always valid
)
