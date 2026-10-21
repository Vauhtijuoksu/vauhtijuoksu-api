package fi.vauhtijuoksu.vauhtijuoksuapi.server.impl.charities

import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Charity
import java.util.UUID

data class NewCharityApiModel(
    val name: String,
    val url: String?,
    val goal: Int?,
    @JsonProperty("img_filename")
    val imgFilename: String?,
) {
    fun toCharity(id: UUID): Charity {
        return Charity(
            id,
            name,
            url,
            goal,
            imgFilename,
        )
    }
}
