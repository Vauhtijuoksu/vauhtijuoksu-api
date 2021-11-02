package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.Mapper
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PatchInputValidator
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PostInputValidator
import io.vertx.ext.web.handler.AuthenticationHandler
import io.vertx.ext.web.handler.CorsHandler
import javax.inject.Inject
import javax.inject.Named

class DonationsRouter
@Inject
constructor(
    db: VauhtijuoksuDatabase<Donation>,
    authenticationHandler: AuthenticationHandler,
    @Named("authenticated")
    authenticatedEndpointCorsHandler: CorsHandler,
    @Named("public")
    publicEndpointCorsHandler: CorsHandler,
    postInputValidator: PostInputValidator<Donation>?,
    patchInputValidator: PatchInputValidator<Donation>?,
) : AbstractRouter<Donation>(
    "/donations",
    Mapper { json -> json.mapTo(Donation::class.java) },
    db,
    authenticationHandler,
    authenticatedEndpointCorsHandler,
    publicEndpointCorsHandler,
    allowPost = true,
    allowDelete = true,
    allowPatch = true,
    postInputValidator,
    patchInputValidator,
)
