package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.models.GameData

class GameDataInputValidator {
    /**
     * Validate gamedata. Returns a string describing any errors found
     */
    @Suppress("ReturnCount") // It's clearer to return as soon as an error is encountered
    fun validateInput(gd: GameData): String? {
        if (gd.id != null) {
            return "ID must not be provided in input"
        }

        for (
            (getter, fieldName) in mapOf<() -> Any?, String>(
                gd::game to "game",
                gd::player to "player",
                gd::startTime to "start_time",
                gd::endTime to "end_time",
                gd::category to "category",
                gd::device to "device",
                gd::published to "published",
            )
        ) {
            val v = getter()

            // Fancy kotlin null check
            v ?: return "$fieldName can't be null"

            if (v is String && v.isEmpty()) {
                return "$fieldName can't be empty"
            }
        }

        if (gd.startTime?.after(gd.endTime) == true) {
            return "Start time must be earlier than end time"
        }
        return null
    }
}
