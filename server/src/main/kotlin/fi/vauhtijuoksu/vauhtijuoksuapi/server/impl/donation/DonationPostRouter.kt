package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.donation

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants.Companion.AUTHENTICATED_CORS
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.PostRouter
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.CorsHandler
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

class DonationPostRouter
@Inject constructor(
    authenticationHandler: AuthenticationHandler,
    @Named(AUTHENTICATED_CORS)
    authenticatedEndpointCorsHandler: CorsHandler,
    db: VauhtijuoksuDatabase<Donation>,
    postInputValidator: DonationPostInputValidator,
) : PostRouter<Donation>(
    authenticationHandler,
    authenticatedEndpointCorsHandler,
    db,
    { json -> json.mapTo(NewDonationApiModel::class.java).toDonation(UUID.randomUUID()) },
    { donation -> DonationApiModel.fromDonation(donation).toJson() },
    postInputValidator,
)
