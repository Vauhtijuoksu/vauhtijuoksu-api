package fi.vauhtijuoksu.vauhtijuoksuapi.database.models

import com.fasterxml.jackson.annotation.JsonProperty
import fi.vauhtijuoksu.vauhtijuoksuapi.models.Charity
import java.util.UUID

data class CharityDbModel(
    val id: UUID,
    val name: String,
    val url: String?,
    val goal: Int?,
    @JsonProperty("img_filename")
    val imgFilename: String?,
) {
    fun toCharity(): Charity {
        return Charity(
            id,
            name,
            url,
            goal,
            imgFilename,
        )
    }

    companion object {
        fun fromCharity(charity: Charity): CharityDbModel {
            return CharityDbModel(
                charity.id,
                charity.name,
                charity.url,
                charity.goal,
                charity.imgFilename,
            )
        }
    }
}
