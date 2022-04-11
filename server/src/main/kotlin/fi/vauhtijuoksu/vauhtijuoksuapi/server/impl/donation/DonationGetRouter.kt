package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.donation

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants.Companion.PUBLIC_CORS
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.GetRouter
import io.vertx.ext.web.handler.CorsHandler
import javax.inject.Inject
import javax.inject.Named

class DonationGetRouter
@Inject constructor(
    @Named(PUBLIC_CORS)
    publicEndpointCorsHandler: CorsHandler,
    db: VauhtijuoksuDatabase<Donation>
) : GetRouter<Donation>(
    publicEndpointCorsHandler,
    db,
    { donation: Donation -> DonationApiModel.fromDonation(donation).toJson() },
)
