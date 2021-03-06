package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiModel
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.PatchRouter
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.CorsHandler
import javax.inject.Inject
import javax.inject.Named

class IncentivePatchRouter
@Inject constructor(
    authenticationHandler: AuthenticationHandler,
    @Named(DependencyInjectionConstants.AUTHENTICATED_CORS)
    private val authenticatedEndpointCorsHandler: CorsHandler,
    db: VauhtijuoksuDatabase<Incentive>,
) : PatchRouter<Incentive, ApiModel<Incentive>>(
    authenticationHandler,
    authenticatedEndpointCorsHandler,
    db,
    { null }, // Incentive is always valid
    IncentiveApiModel::fromIncentive
)
