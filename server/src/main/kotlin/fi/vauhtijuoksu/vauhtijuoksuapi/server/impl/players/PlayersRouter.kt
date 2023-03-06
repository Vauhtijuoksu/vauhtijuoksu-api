package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.players

import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.BaseRouter
import javax.inject.Inject

class PlayersRouter
@Inject constructor(
    getPlayersRouter: GetPlayersRouter,
    addPlayerRouter: AddPlayerRouter,
    modifyPlayerRouter: ModifyPlayerRouter,
    removePLayerRouter: RemovePLayerRouter,
) : BaseRouter(
    "/players",
    listOf(
        getPlayersRouter,
        addPlayerRouter,
        modifyPlayerRouter,
        removePLayerRouter,
    ),
)
