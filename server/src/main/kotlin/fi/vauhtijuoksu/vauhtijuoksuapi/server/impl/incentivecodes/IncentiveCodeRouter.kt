package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.incentivecodes

import fi.vauhtijuoksu.vauhtijuoksuapi.models.ChosenIncentive
import fi.vauhtijuoksu.vauhtijuoksuapi.server.ApiConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.DependencyInjectionConstants
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PartialRouter
import fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.base.BaseRouter
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

data class ChosenIncentiveApiModel(
    val id: UUID,
    val parameter: String?,
)

data class GeneratedCodeApiModel(
    val code: String,
)

class IncentiveCodePostRouter @Inject constructor(
    @Named(DependencyInjectionConstants.PUBLIC_CORS)
    private val publicEndpointCorsHandler: CorsHandler,
    private val incentiveCodeService: IncentiveCodeService,
) : PartialRouter {
    override fun configure(router: Router, basepath: String) {
        router.post(basepath)
            .handler(publicEndpointCorsHandler)
            .handler(BodyHandler.create())
            .handler { ctx ->
                val codes = ctx.bodyAsJsonArray
                    .map { (it as JsonObject).mapTo(ChosenIncentiveApiModel::class.java) }
                    .map { ChosenIncentive(it.id, it.parameter) }
                incentiveCodeService.generateCode(codes)
                    .onFailure(ctx::fail)
                    .onSuccess {
                        ctx.response()
                            .setStatusCode(ApiConstants.CREATED)
                            .end(JsonObject.mapFrom(GeneratedCodeApiModel(it.code)).encode())
                    }
            }
    }
}

class IncentiveCodeRouter @Inject constructor(
    incentiveCodePostRouter: IncentiveCodePostRouter,
) : BaseRouter(
    "/generate-incentive-code",
    listOf(incentiveCodePostRouter)
)
