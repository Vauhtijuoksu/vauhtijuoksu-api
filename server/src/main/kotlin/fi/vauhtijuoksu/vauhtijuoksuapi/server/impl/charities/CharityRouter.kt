package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.charities

import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.BaseRouter
import jakarta.inject.Inject

class CharityRouter
@Inject
constructor(
    getRouter: CharityGetRouter,
    postRouter: CharityPostRouter,
    patchRouter: CharityPatchRouter,
    deleteRouter: CharityDeleteRouter,
) : BaseRouter(
    "/charities",
    listOf(
        getRouter,
        postRouter,
        patchRouter,
        deleteRouter,
    ),
)
