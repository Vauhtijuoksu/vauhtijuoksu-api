package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.Mapper
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PostInputValidator
import io.vertx.ext.web.handler.AuthenticationHandler
import javax.inject.Inject

class DonationsRouter
@Inject
constructor(
    db: VauhtijuoksuDatabase<Donation>,
    authenticationHandler: AuthenticationHandler,
    postInputValidator: PostInputValidator<Donation>?
) : AbstractRouter<Donation>(
    "/donations",
    Mapper { json -> json.mapTo(Donation::class.java) },
    db,
    authenticationHandler,
    allowPost = true,
    allowDelete = true,
    allowPatch = true,
    postInputValidator
)
