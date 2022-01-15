package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.gamedata

import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PatchInputValidator

class GameDataPatchInputValidator : GameDataInputValidator(), PatchInputValidator<GameData> {
    override fun validate(input: GameData): String? {
        return validateFields(input)
    }
}
