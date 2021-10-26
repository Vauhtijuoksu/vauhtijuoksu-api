package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation

@Suppress("ReturnCount") // It's cleaner to stop validation right away and return at error
open class DonationInputValidator {
    fun validateFields(input: Donation): String? {
        val errors = validateNonNullFields(input)
        if (errors != null) {
            return errors
        }

        // Verified to be non-null above
        if (input.amount!! < 0) {
            return "No stealing from norppas!!"
        }
        return null
    }

    private fun validateNonNullFields(input: Donation): String? {
        for (
            (getter, fieldName) in mapOf<() -> Any?, String>(
                input::name to "name",
                input::timestamp to "timestamp",
                input::amount to "amount",
                input::read to "read",
            )
        ) {
            val v = getter()

            // Fancy kotlin null check
            v ?: return "$fieldName can't be null"

            if (v is String && v.isEmpty()) {
                return "$fieldName can't be empty"
            }
        }
        return null
    }
}
