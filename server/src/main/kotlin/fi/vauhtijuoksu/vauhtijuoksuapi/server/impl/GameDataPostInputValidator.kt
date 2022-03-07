package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PostInputValidator

class GameDataPostInputValidator : GameDataInputValidator(), PostInputValidator<GameData> {
    override
    fun validate(input: GameData): String? {
        if (input.id != null) {
            return "ID must not be provided in input"
        }
        return validateFields(input)
    }
}
