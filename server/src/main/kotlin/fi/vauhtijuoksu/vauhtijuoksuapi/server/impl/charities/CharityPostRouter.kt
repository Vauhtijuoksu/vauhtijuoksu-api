package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.charities

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Charity
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.PostRouter
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.AuthorizationHandler
import io.vertx.ext.web.handler.CorsHandler
import jakarta.inject.Inject
import jakarta.inject.Named
import java.util.UUID

class CharityPostRouter
@Inject constructor(
    authenticationHandler: AuthenticationHandler,
    adminRequired: AuthorizationHandler,
    @Named(DependencyInjectionConstants.AUTHENTICATED_CORS)
    authenticatedEndpointCorsHandler: CorsHandler,
    db: VauhtijuoksuDatabase<Charity>,
) : PostRouter<Charity>(
    authenticationHandler,
    adminRequired,
    authenticatedEndpointCorsHandler,
    db,
    toModel = { json -> json.mapTo(NewCharityApiModel::class.java).toCharity(UUID.randomUUID()) },
    toJson = { charity -> CharityApiModel.from(charity).toJson() },
    postInputValidator = { null },
)
