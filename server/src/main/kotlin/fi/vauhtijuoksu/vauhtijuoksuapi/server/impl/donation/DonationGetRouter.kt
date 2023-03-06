package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.donation

import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.ServerError
import fi.vauhtijuoksu.vauhtijuoksuapi.exceptions.UserError
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PartialRouter
import io.vertx.core.json.JsonArray
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.CorsHandler
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

class DonationGetRouter
@Inject constructor(
    @Named(DependencyInjectionConstants.PUBLIC_CORS) private val publicEndpointCorsHandler: CorsHandler,
    private val donationService: DonationService,
) : PartialRouter {
    private val toJson = { donation: DonationWithCodes -> DonationApiModel.fromDonationWithCodes(donation).toJson() }

    override fun configure(router: Router, basepath: String) {
        configureGetAll(router, basepath)
        configureGetSingle(router, basepath)
    }

    private fun configureGetAll(router: Router, basepath: String) {
        router.get(basepath).handler(publicEndpointCorsHandler).handler { ctx ->
            donationService.getDonations().onFailure { t ->
                ctx.fail(ServerError("Failed to retrieve record because of ${t.message}"))
            }.onSuccess { all ->
                val res = JsonArray(all.map(toJson))
                ctx.response().end(res.encode())
            }
        }
    }

    private fun configureGetSingle(router: Router, basepath: String) {
        router.get("$basepath/:id").handler(publicEndpointCorsHandler).handler { ctx ->
            val id: UUID
            try {
                id = UUID.fromString(ctx.pathParam("id"))
            } catch (_: IllegalArgumentException) {
                throw UserError("Not UUID: ${ctx.pathParam("id")}")
            }
            donationService.getDonation(id).onFailure(ctx::fail).onSuccess { res ->
                ctx.response().end(toJson(res).encode())
            }
        }
    }
}
