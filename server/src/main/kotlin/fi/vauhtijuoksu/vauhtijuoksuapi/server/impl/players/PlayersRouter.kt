package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.players

import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.BaseRouter
import jakarta.inject.Inject

class PlayersRouter
@Inject constructor(
    getPlayersRouter: GetPlayersRouter,
    addPlayerRouter: AddPlayerRouter,
    modifyPlayerRouter: ModifyPlayerRouter,
    removePLayerRouter: RemovePlayerRouter,
) : BaseRouter(
    "/players",
    listOf(
        getPlayersRouter,
        addPlayerRouter,
        modifyPlayerRouter,
        removePLayerRouter,
    ),
)
