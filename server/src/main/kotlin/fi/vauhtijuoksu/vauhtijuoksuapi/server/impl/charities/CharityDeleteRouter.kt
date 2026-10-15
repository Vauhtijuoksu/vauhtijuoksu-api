package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.charities

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Charity
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.DeleteRouter
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.AuthorizationHandler
import io.vertx.ext.web.handler.CorsHandler
import jakarta.inject.Inject
import jakarta.inject.Named

class CharityDeleteRouter
@Inject constructor(
    authenticationHandler: AuthenticationHandler,
    adminRequired: AuthorizationHandler,
    @Named(DependencyInjectionConstants.AUTHENTICATED_CORS)
    authenticatedEndpointCorsHandler: CorsHandler,
    db: VauhtijuoksuDatabase<Charity>,
) : DeleteRouter<Charity>(
    authenticationHandler,
    adminRequired,
    authenticatedEndpointCorsHandler,
    db,
)
