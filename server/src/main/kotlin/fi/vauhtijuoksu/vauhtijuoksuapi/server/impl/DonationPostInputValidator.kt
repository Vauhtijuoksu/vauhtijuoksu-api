package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PostInputValidator

class DonationPostInputValidator : DonationInputValidator(), PostInputValidator<Donation> {
    override fun validate(input: Donation): String? {
        if (input.id != null) {
            return "ID must not be provided in input"
        }
        return validateFields(input)
    }
}
