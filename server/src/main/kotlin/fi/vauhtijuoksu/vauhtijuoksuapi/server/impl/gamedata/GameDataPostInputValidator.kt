package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.gamedata

import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PostInputValidator

class GameDataPostInputValidator : GameDataInputValidator(), PostInputValidator<GameData> {
    override
    fun validate(input: GameData): String? {
        return validateFields(input)
    }
}
