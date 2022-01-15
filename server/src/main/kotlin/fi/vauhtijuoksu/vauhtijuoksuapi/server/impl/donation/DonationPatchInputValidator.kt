package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.donation

import fi.vauhtijuoksu.vauhtijuoksuapi.models.Donation
import fi.vauhtijuoksu.vauhtijuoksuapi.server.api.PatchInputValidator

class DonationPatchInputValidator : DonationInputValidator(), PatchInputValidator<Donation> {
    override fun validate(input: Donation): String? {
        return validateFields(input)
    }
}
