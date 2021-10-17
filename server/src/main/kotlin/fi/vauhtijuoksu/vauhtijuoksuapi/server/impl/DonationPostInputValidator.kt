package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PostInputValidator

class DonationPostInputValidator : PostInputValidator<Donation> {
    @Suppress("ReturnCount") // It's cleaner to stop validation right away and return at error
    override fun validate(input: Donation): String? {
        if (input.id != null) {
            return "ID must not be provided in input"
        }

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

        // Verified to be non-null above
        if (input.amount!! < 0) {
            return "No stealing from norppas!!"
        }
        return null
    }
}
