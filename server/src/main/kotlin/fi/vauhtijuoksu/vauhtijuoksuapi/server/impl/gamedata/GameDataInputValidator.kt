package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.gamedata

import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData

@Suppress("ReturnCount") // It's cleaner to stop validation right away and return at error
open class GameDataInputValidator {
    fun validateFields(input: GameData): String? {
        val errors = validateNonNullFields(input)
        if (errors != null) {
            return errors
        }
        return null
    }

    private fun validateNonNullFields(input: GameData): String? {

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
