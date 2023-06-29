package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.gamedata

import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.BaseRouter
import jakarta.inject.Inject

class GameDataRouter @Inject constructor(
    getRouter: GameDataGetRouter,
    postRouter: GameDataPostRouter,
    patchRouter: GameDataPatchRouter,
    deleteRouter: GameDataDeleteRouter,
) :
    BaseRouter(
        "/gamedata",
        listOf(
            getRouter,
            postRouter,
            patchRouter,
            deleteRouter,
        ),
    )
