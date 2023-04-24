package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.timers

import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.BaseRouter
import javax.inject.Inject

class TimerRouter
@Inject
constructor(
    getRouter: TimerGetRouter,
    postRouter: TimerPostRouter,
    patchRouter: TimerPatchRouter,
    deleteRouter: TimerDeleteRouter,
) : BaseRouter(
    "/timers",
    listOf(
        getRouter,
        postRouter,
        patchRouter,
        deleteRouter,
    ),
)
