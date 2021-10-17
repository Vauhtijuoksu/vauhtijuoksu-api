package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PostInputValidator

class GameDataPostInputValidator : PostInputValidator<GameData> {
    /**
     * Validate gamedata. Returns a string describing any errors found
     */
    @Suppress("ReturnCount")
    override // It's clearer to return as soon as an error is encountered
    fun validate(input: GameData): String? {
        if (input.id != null) {
            return "ID must not be provided in input"
        }

        for (
            (getter, fieldName) in mapOf<() -> Any?, String>(
                input::game to "game",
                input::player to "player",
                input::startTime to "start_time",
                input::endTime to "end_time",
                input::category to "category",
                input::device to "device",
                input::published to "published",
            )
        ) {
            val v = getter()

            // Fancy kotlin null check
            v ?: return "$fieldName can't be null"

            if (v is String && v.isEmpty()) {
                return "$fieldName can't be empty"
            }
        }

        if (input.startTime?.after(input.endTime) == true) {
            return "Start time must be earlier than end time"
        }
        return null
    }
}
