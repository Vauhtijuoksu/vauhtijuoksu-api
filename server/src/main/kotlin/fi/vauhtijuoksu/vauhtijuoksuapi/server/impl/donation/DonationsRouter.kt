package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.donation

import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.BaseRouter
import javax.inject.Inject

class DonationsRouter
@Inject
constructor(
    getRouter: DonationGetRouter,
    postRouter: DonationPostRouter,
    patchRouter: DonationPatchRouter,
    deleteRouter: DonationDeleteRouter,
) : BaseRouter(
    "/donations",
    listOf(
        getRouter,
        postRouter,
        patchRouter,
        deleteRouter,
    ),
)
