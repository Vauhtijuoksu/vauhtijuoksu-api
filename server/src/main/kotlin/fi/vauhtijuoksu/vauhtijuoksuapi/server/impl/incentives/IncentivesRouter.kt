package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentives

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.BaseRouter
import io.vertx.core.json.jackson.DatabindCodec
import jakarta.inject.Inject

class IncentivesRouter
@Inject constructor(
    getRouter: IncentiveGetRouter,
    postRouter: IncentivePostRouter,
    patchRouter: IncentivePatchRouter,
    deleteRouter: IncentiveDeleteRouter,
) : BaseRouter(
    "/incentives",
    listOf(
        getRouter,
        postRouter,
        patchRouter,
        deleteRouter,
    ),
) {
    init {
        DatabindCodec.mapper().registerModule(JavaTimeModule())
    }
}
