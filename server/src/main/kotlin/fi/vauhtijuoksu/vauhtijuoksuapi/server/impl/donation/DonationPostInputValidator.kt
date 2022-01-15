package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.donation

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PostInputValidator

class DonationPostInputValidator : DonationInputValidator(), PostInputValidator<Donation> {
    override fun validate(input: Donation): String? {
        return validateFields(input)
    }
}
