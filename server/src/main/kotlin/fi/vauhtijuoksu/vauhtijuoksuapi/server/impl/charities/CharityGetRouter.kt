package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.charities

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Charity
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.GetRouter
import io.vertx.ext.web.handler.CorsHandler
import jakarta.inject.Inject
import jakarta.inject.Named

class CharityGetRouter
@Inject constructor(
    @Named(DependencyInjectionConstants.PUBLIC_CORS)
    publicEndpointCorsHandler: CorsHandler,
    db: VauhtijuoksuDatabase<Charity>,
) : GetRouter<Charity>(
    publicEndpointCorsHandler,
    db,
    toJson = { charity -> CharityApiModel.from(charity).toJson() },
)
