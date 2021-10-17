package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.database.api.VauhtijuoksuDatabase
import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.Mapper
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PostInputValidator
import io.vertx.ext.web.handler.AuthenticationHandler
import javax.inject.Inject

class GameDataRouter @Inject constructor(
    db: VauhtijuoksuDatabase<GameData>,
    inputValidator: PostInputValidator<GameData>,
    authenticationHandler: AuthenticationHandler
) :
    AbstractRouter<GameData>(
        "/gamedata",
        Mapper { json -> json.mapTo(GameData::class.java) },
        db,
        authenticationHandler,
        allowPost = true,
        allowDelete = true,
        allowPatch = true,
        inputValidator
    )
