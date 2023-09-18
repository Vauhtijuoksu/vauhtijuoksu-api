package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.server.ApiConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.ApiModel
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.PatchRouter
import io.vertx.core.Future
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.AuthorizationHandler
import io.vertx.ext.web.handler.CorsHandler
import jakarta.inject.Inject
import jakarta.inject.Named
import mu.KotlinLogging
import java.util.UUID

class IncentivePatchRouter
@Inject constructor(
    authenticationHandler: AuthenticationHandler,
    adminRequired: AuthorizationHandler,
    @Named(DependencyInjectionConstants.AUTHENTICATED_CORS)
    private val authenticatedEndpointCorsHandler: CorsHandler,
    db: VauhtijuoksuDatabase<Incentive>,
    private val incentiveService: IncentiveService,
) : PatchRouter<Incentive, ApiModel<Incentive>>(
    authenticationHandler,
    adminRequired,
    authenticatedEndpointCorsHandler,
    db,
    { null }, // Incentive is always valid
    IncentiveApiModel::fromIncentive,
) {
    private val logger = KotlinLogging.logger {}
    override fun respond(updatedId: UUID, ctx: RoutingContext): Future<Void> {
        return incentiveService
            .getIncentive(updatedId)
            .map {
                logger.info { "Patched record $it" }
                ctx.response().setStatusCode(ApiConstants.OK)
                    .end(IncentiveApiModel.fromIncentiveWithStatuses(it).toJson().encode())
            }
            .mapEmpty()
    }
}
