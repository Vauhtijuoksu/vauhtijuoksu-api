package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Incentive
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.GetRouter
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.ext.web.handler.CorsHandler
import javax.inject.Inject
import javax.inject.Named

class IncentiveGetRouter
@Inject constructor(
    @Named(DependencyInjectionConstants.PUBLIC_CORS) publicEndpointCorsHandler: CorsHandler,
    incentiveDatabase: VauhtijuoksuDatabase<Incentive>
) :
    GetRouter<Incentive>(
        publicEndpointCorsHandler,
        incentiveDatabase,
        { incentive -> IncentiveApiModel.fromIncentive(incentive).toJson() }
    ) {
    init {
        DatabindCodec.mapper().registerModule(JavaTimeModule())
    }
}
